package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.dao.UserEntity
import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import com.vdsirotkin.telegram.mystickersbot.service.PngService
import com.vdsirotkin.telegram.mystickersbot.util.addInlineKeyboard
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.monoWithMdc
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        private val props: BotConfigProps
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> {
        val chatId = update.message!!.chat.id
        return monoWithMdc {
            val entity = dao.getUserEntity(chatId)
            val messageId = update.message!!.messageId
            try {
                val sticker = update.message!!.sticker!!
                logDebug(sticker.toString())
                if (entity.normalPackCreated) {
                    addStickerToPack(bot, messageId, chatId, sticker, entity)
                } else {
                    createNewPack(bot, messageId, chatId, sticker, entity)
                }
            } catch (e: PngNotCreatedException) {
                logger.warn("Can't create png from this sticker")
                bot.executeAsync(SendMessage(chatId, "Sorry, this sticker can't be processed. Please try another one.").setReplyToMessageId(messageId))
            } catch (e: Exception) {
                throw e
            }
        }.thenReturn(Unit)
    }

    private suspend fun createNewPack(bot: DefaultAbsSender,
                                      messageId: Int,
                                      chatId: Long,
                                      sticker: Sticker,
                                      entity: UserEntity) {
        logDebug("test0")
        withTempFile(preparePngFile(bot, sticker)) {
            bot.execute(CreateNewStickerSet(chatId.toInt(), entity.normalPackName, "Your stickers - @${props.username}", sticker.emoji!!).setPngStickerFile(it)
                    .apply {
                        containsMasks = sticker.maskPosition != null
                        maskPosition = sticker.maskPosition
                    }
            )
        }
        dao.setCreatedStatus(chatId, normalStickerCreated = true)
        bot.executeAsync(SendMessage(chatId,
                "Successfully created sticker pack and added this sticker to it :)")
                .setReplyToMessageId(messageId)
                .addInlineKeyboard("Sticker pack", "https://t.me/addstickers/${entity.normalPackName}"))
    }

    private suspend fun addStickerToPack(bot: DefaultAbsSender,
                                         messageId: Int,
                                         chatId: Long,
                                         sticker: Sticker,
                                         entity: UserEntity) {
        withTempFile(preparePngFile(bot, sticker)) {
            bot.execute(AddStickerToSet(chatId.toInt(), entity.normalPackName, sticker.emoji!!).setPngSticker(it))
        }
        bot.executeAsync(SendMessage(chatId, "This sticker added!")
                .setReplyToMessageId(messageId)
                .addInlineKeyboard("Sticker pack", "https://t.me/addstickers/${entity.normalPackName}")
        )
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
