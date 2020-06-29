package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.dao.UserEntity
import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.PngService
import com.vdsirotkin.telegram.mystickersbot.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

@Service
class NormalStickerHandler(
        private val dao: StickerDAO,
        private val pngService: PngService,
        private val props: BotConfigProps,
        override val stickerDao: StickerDAO,
        override val messageSource: MessageSource
) : LocalizedHandler {

    override fun handleInternal(bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper): Mono<Unit> {
        val chatId = update.message!!.chat.id
        return mdcMono {
            val entity = dao.getUserEntity(chatId)
            val messageId = update.message!!.messageId
            val sticker = update.message!!.sticker!!
            if (dao.stickerExists(chatId, sticker.fileUniqueId, false)) {
                bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.already.added")).setReplyToMessageId(update.message!!.messageId))
                return@mdcMono
            }
            try {
                logDebug(sticker.toString())
                if (entity.normalPackCreated) {
                    addStickerToPack(bot, messageId, chatId, sticker, entity, messageSource)
                } else {
                    createNewPack(bot, messageId, chatId, sticker, entity, messageSource)
                }
                dao.saveSticker(chatId, sticker, false)
            } catch (e: PngNotCreatedException) {
                logger.warn("Can't create png from this sticker")
                bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.cant.be.processed")).setReplyToMessageId(messageId))
            } catch (e: Exception) {
                throw e
            }
        }.thenReturn(Unit)
    }

    private suspend fun addStickerToPack(bot: DefaultAbsSender,
                                         messageId: Int,
                                         chatId: Long,
                                         sticker: Sticker,
                                         entity: UserEntity,
                                         messageSource: MessageSourceWrapper) {
        withTempFile(preparePngFile(bot, sticker)) {
            bot.execute(AddStickerToSet(chatId.toInt(), entity.normalPackName, sticker.emoji ?: "🙂").setPngSticker(it))
        }
        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.added"))
                .setReplyToMessageId(messageId)
                .addInlineKeyboard(messageSource.getMessage("sticker.pack.button.text"), "https://t.me/addstickers/${entity.normalPackName}")
        )
    }

    private suspend fun createNewPack(bot: DefaultAbsSender,
                                      messageId: Int,
                                      chatId: Long,
                                      sticker: Sticker,
                                      entity: UserEntity,
                                      messageSource: MessageSourceWrapper) {
        withTempFile(preparePngFile(bot, sticker)) {
            bot.execute(CreateNewStickerSet(chatId.toInt(), entity.normalPackName, "Your stickers - @${props.username}", sticker.emoji ?: "🙂").setPngStickerFile(it)
                    .apply {
                        containsMasks = sticker.maskPosition != null
                        maskPosition = sticker.maskPosition
                    }
            )
        }
        dao.setCreatedStatus(chatId, normalStickerCreated = true)
        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("created.pack")
        )
                .setReplyToMessageId(messageId)
                .addInlineKeyboard(messageSource.getMessage("sticker.pack.button.text"), "https://t.me/addstickers/${entity.normalPackName}"))
    }

    private suspend fun preparePngFile(bot: DefaultAbsSender,
                                       sticker: Sticker): File {
        val file = withContext(Dispatchers.IO) { Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".webp").toFile() }
        return withTempFile(file) { webpFile ->
            val stickerFile = bot.executeAsync(GetFile().setFileId(sticker.fileId))
            bot.downloadFile(stickerFile, webpFile)
            pngService.convertToPng(webpFile)
        }
    }

    companion object : Loggable
}
