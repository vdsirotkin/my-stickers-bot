package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.util.*
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.stickers.AddStickerToSet
import org.telegram.telegrambots.meta.api.methods.stickers.CreateNewStickerSet
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker
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

    override fun handleInternal(bot: DefaultAbsSender, update: Update,
                                messageSource: MessageSourceWrapper): Mono<Unit> = mdcMono {
        val chatId = update.message!!.chat.id
        val sticker = update.message!!.sticker!!
        logger.info(sticker.toString())

        val entity = dao.getUserEntity(chatId)
        if (dao.stickerExists(chatId, sticker.fileUniqueId, true)) {
            bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.already.added")).setReplyToMessageId(update.message!!.messageId))
            return@mdcMono
        }
        val stickerFile = fileHelper.downloadFile(bot, sticker.fileId)
        if (entity.animatedPackCreated) {
            optimizeIfNecessary(stickerFile) {
                bot.wrapApiCall {
                    bot.execute(AddStickerToSet(chatId.toInt(), entity.animatedPackName, sticker.emoji ?: "🙂")
                            .setTgsSticker(it)
                            .setMaskPosition(sticker.maskPosition)
                    )
                }
            }
            bot.executeAsync(
                    SendMessage(chatId, messageSource.getMessage("sticker.added"))
                            .setReplyToMessageId(update.message!!.messageId)
                            .addInlineKeyboard(messageSource.getMessage("animated.sticker.pack.button.text"), "https://t.me/addstickers/${entity.animatedPackName}")
            )
        } else {
            optimizeIfNecessary(stickerFile) {
                bot.wrapApiCall {
                    bot.execute(CreateNewStickerSet(chatId.toInt(), entity.animatedPackName, "Your animated stickers - @${props.username}", sticker.emoji ?: "🙂")
                            .setTgsSticker(it)
                            .apply { containsMasks = sticker.maskPosition != null; maskPosition = sticker.maskPosition }
                    )
                }
            }
            dao.setCreatedStatus(chatId, animatedStickerCreated = true)
            bot.executeAsync(
                    SendMessage(chatId, messageSource.getMessage("created.pack"))
                            .setReplyToMessageId(update.message!!.messageId)
                            .addInlineKeyboard(messageSource.getMessage("animated.sticker.pack.button.text"), "https://t.me/addstickers/${entity.animatedPackName}")
            )
        }
        dao.saveSticker(chatId, sticker, true)
    }.thenReturn(Unit)

    private suspend fun getStickerFile(bot: DefaultAbsSender,
                                       sticker: Sticker): File {
        val file = bot.executeAsync(GetFile().setFileId(sticker.fileId))
        logger.info(file.toString())
        return bot.downloadFileAsync(file.filePath)
    }

    private suspend fun optimizeIfNecessary(originalFile: File, block: suspend (File) -> Unit) {
        withTempFile(originalFile) {
            // step one - try naive way (handles most of cases actually)
            val success = kotlin.runCatching { block(originalFile) }.isSuccess
            if (success) return@withTempFile
            logDebug("First step for animated sticker failed! Trying second way")

            // step two - optimize with lottie (python, bruh)
            val tempOriginalPath = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".tgs")
            Files.copy(originalFile.toPath(), tempOriginalPath, StandardCopyOption.REPLACE_EXISTING)
            val tempJsonFile = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".json").toFile()
            val tempOriginalFile = tempOriginalPath.toFile()
            val newTgsFilePath = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".tgs")
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
