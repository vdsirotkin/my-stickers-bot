package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.PngService
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
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
        private val stickerPackManagementService: StickerPackManagementService,
        private val stickerPackMessagesSender: StickerPackMessagesSender,
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
                withTempFile(preparePngFile(bot, sticker)) {
                    if (entity.normalPackCreated) {
                        stickerPackManagementService.addStickerToPack(bot, chatId, it, entity, sticker.emoji)
                        stickerPackMessagesSender.sendSuccessAdd(bot, chatId, messageSource, messageId, entity)
                    } else {
                        stickerPackManagementService.createNewPack(bot, chatId, it, entity)
                        dao.setCreatedStatus(chatId, normalStickerCreated = true)
                        stickerPackMessagesSender.sendSuccessCreated(bot, chatId, messageSource, messageId, entity)
                    }
                    dao.saveSticker(chatId, sticker, false)
                }
            } catch (e: PngNotCreatedException) {
                logger.warn("Can't create png from this sticker")
                bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.cant.be.processed")).setReplyToMessageId(messageId))
            } catch (e: Exception) {
                throw e
            }
        }.thenReturn(Unit)
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
