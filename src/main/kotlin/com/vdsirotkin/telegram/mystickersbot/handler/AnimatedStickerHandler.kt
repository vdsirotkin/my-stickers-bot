package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.util.addInlineKeyboard
import com.vdsirotkin.telegram.mystickersbot.util.downloadFileAsync
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.stickers.AddStickerToSet
import org.telegram.telegrambots.meta.api.methods.stickers.CreateNewStickerSet
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono

@Service
class AnimatedStickerHandler(
        private val dao: StickerDAO
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> = mono {
        val chatId = update.message!!.chat.id
        val sticker = update.message!!.sticker!!

        val entity = dao.getUserEntity(chatId)
        if (entity.animatedPackCreated) {
            withTempFile(bot.downloadFileAsync(sticker.fileId)) {
                bot.execute(AddStickerToSet(chatId.toInt(), entity.animatedPackName, sticker.emoji!!).setTgsSticker(it))
            }
            bot.executeAsync(
                    SendMessage(chatId, "Successfully added :)")
                            .setReplyToMessageId(update.message!!.messageId)
                            .addInlineKeyboard("Sticker pack", "https://t.me/addstickers/${entity.animatedPackName}")
            )
        } else {
            withTempFile(bot.downloadFileAsync(sticker.fileId)) {
                bot.execute(CreateNewStickerSet(chatId.toInt(), entity.animatedPackName, "Your animated stickers - @my_stckrs_bot", sticker.emoji!!).setTgsSticker(it))
            }
            dao.setCreatedStatus(chatId, animatedStickerCreated = true)
            bot.executeAsync(
                    SendMessage(chatId, "Successfully created sticker pack and added this sticker to it :)")
                            .setReplyToMessageId(update.message!!.messageId)
                            .addInlineKeyboard("Sticker pack", "https://t.me/addstickers/${entity.animatedPackName}")
            )
        }
        Unit
    }

}
