package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.AddStickerToSet
import com.pengrad.telegrambot.request.CreateNewStickerSet
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.util.*
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Service
class AnimatedStickerHandler(
        private val dao: StickerDAO,
        private val props: BotConfigProps,
        override val stickerDao: StickerDAO,
        override val messageSource: MessageSource,
        val fileHelper: FileHelper
) : LocalizedHandler {

    override fun handleInternal(
            bot: TelegramBot, update: Update,
            messageSource: MessageSourceWrapper,
            userEntity: UserEntity
    ): Mono<BaseHandler> = mdcMono {
        val chatId = update.message().chat().id()
        val sticker = update.message().sticker()
        logger.info(sticker.toString())

        if (dao.stickerExists(userEntity, sticker, true)) {
            bot.executeAsync(SendMessageWithAction(chatId, messageSource["sticker.already.added"], action).replyToMessageId(update.message().messageId()))
            return@mdcMono
        }
        val stickerFile = fileHelper.downloadFile(bot, sticker.fileId())
        if (userEntity.animatedPackCreated) {
            optimizeIfNecessary(stickerFile) {
                bot.executeAsync(AddStickerToSet.tgsSticker(chatId.toInt(), userEntity.animatedPackName, sticker.emoji() ?: "🙂", it)
                        .apply {
                            if (sticker.maskPosition() != null) {
                                maskPosition(sticker.maskPosition())
                            }
                        })
            }
            bot.executeAsync(
                    SendMessageWithAction(chatId, messageSource["sticker.added"], action)
                            .replyToMessageId(update.message().messageId())
                            .addInlineKeyboard(messageSource["animated.sticker.pack.button.text"], packLink(userEntity.animatedPackName))
            )
        } else {
            optimizeIfNecessary(stickerFile) {
                bot.executeAsync(CreateNewStickerSet.tgsSticker(chatId.toInt(), userEntity.animatedPackName, "Your animated stickers - @${props.username}", sticker.emoji() ?: "🙂", it)
                        .apply {
                            if (sticker.maskPosition() != null) {
                                maskPosition(sticker.maskPosition())
                            }
                        }
                )
            }
            dao.setCreatedStatus(chatId, animatedStickerCreated = true)
            bot.executeAsync(
                    SendMessageWithAction(chatId, messageSource["created.pack"], action)
                            .replyToMessageId(update.message().messageId())
                            .addInlineKeyboard(messageSource["animated.sticker.pack.button.text"], packLink(userEntity.animatedPackName))
            )
        }
        dao.saveSticker(chatId, StickerMeta(sticker.fileId(), sticker.fileUniqueId(), sticker.emoji()), true)
    }.thenReturn(this)

    override val action: String
        get() = "ANIMATED_STICKER"

    private suspend fun optimizeIfNecessary(originalFile: File, block: suspend (File) -> Unit) {
        withTempFile(originalFile) {
            // step one - try naive way (handles most of cases actually)
            val success = kotlin.runCatching { block(originalFile) }.isSuccess
            if (success) return@withTempFile
            logDebug("First step for animated sticker failed! Trying second way")

            // step two - optimize with lottie (python, bruh)
            val tempOriginalPath = Files.createTempFile(TEMP_FILE_PREFIX, TGS_SUFFIX)
            Files.copy(originalFile.toPath(), tempOriginalPath, StandardCopyOption.REPLACE_EXISTING)
            val tempJsonFile = Files.createTempFile(TEMP_FILE_PREFIX, ".json").toFile()
            val tempOriginalFile = tempOriginalPath.toFile()
            val newTgsFilePath = Files.createTempFile(TEMP_FILE_PREFIX, TGS_SUFFIX)
            val newTgsFileAbsolutePath = newTgsFilePath.toFile().absolutePath
            Files.delete(newTgsFilePath)
            withTempFiles(arrayOf(tempJsonFile, tempOriginalFile)) {
                execWithLog("lottie_convert.py ${tempOriginalFile.absolutePath} ${tempJsonFile.absolutePath}")
                execWithLog("lottie_convert.py ${tempJsonFile.absolutePath} ${newTgsFileAbsolutePath}")
                block(File(newTgsFileAbsolutePath))
            }
        }
    }

    companion object : Loggable

}
