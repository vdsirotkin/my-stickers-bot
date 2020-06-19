package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.util.*
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

@Service
class AnimatedStickerHandler(
        private val dao: StickerDAO,
        private val props: BotConfigProps
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> = monoWithMdc {
        val chatId = update.message!!.chat.id
        val sticker = update.message!!.sticker!!
        logger.info(sticker.toString())

        val entity = dao.getUserEntity(chatId)
        if (entity.animatedPackCreated) {
            withTempFile(getStickerFile(bot, sticker)) {
                logger.info(it.absolutePath)
                bot.execute(AddStickerToSet(chatId.toInt(), entity.animatedPackName, sticker.emoji!!)
                        .setTgsSticker(it)
                        .setMaskPosition(sticker.maskPosition)
                )
            }
            bot.executeAsync(
                    SendMessage(chatId, "Successfully added :)")
                            .setReplyToMessageId(update.message!!.messageId)
                            .addInlineKeyboard("Your animated sticker pack", "https://t.me/addstickers/${entity.animatedPackName}")
            )
        } else {
            withTempFile(getStickerFile(bot, sticker)) {
                logger.info(it.absolutePath)
                bot.execute(CreateNewStickerSet(chatId.toInt(), entity.animatedPackName, "Your animated stickers - @${props.username}", sticker.emoji!!)
                        .setTgsSticker(it)
                        .apply { containsMasks = sticker.maskPosition != null; maskPosition = sticker.maskPosition }
                )
            }
            dao.setCreatedStatus(chatId, animatedStickerCreated = true)
            bot.executeAsync(
                    SendMessage(chatId, "Successfully created sticker pack and added this sticker to it :)")
                            .setReplyToMessageId(update.message!!.messageId)
                            .addInlineKeyboard("Your animated sticker pack", "https://t.me/addstickers/${entity.animatedPackName}")
            )
        }
    }.thenReturn(Unit)

    private suspend fun getStickerFile(bot: DefaultAbsSender,
                                       sticker: Sticker): File {
        val file = bot.executeAsync(GetFile().setFileId(sticker.fileId))
        logger.info(file.toString())
        return bot.downloadFileAsync(file.filePath)
    }

    companion object : Loggable

}
