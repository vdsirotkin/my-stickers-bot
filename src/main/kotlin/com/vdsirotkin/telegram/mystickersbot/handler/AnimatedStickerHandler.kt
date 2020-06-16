package com.vdsirotkin.telegram.mystickersbot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.stickers.StickerType
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.util.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import ru.sokomishalov.commons.core.common.unit
import java.nio.file.Files

@Service
class AnimatedStickerHandler(
        private val dao: StickerDAO
) : BaseHandler {

    override fun handle(bot: Bot, update: Update) = runBlocking(Dispatchers.IO) {
        val chatId = update.message!!.chat.id
        val sticker = update.message!!.sticker!!
        val entity = dao.getUserEntity(chatId)
        if (entity.animatedPackCreated) {
//            addStickerToPack(bot, update, entity)
            val bytes = bot.downloadFileBytes(sticker.fileId)
            val path = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", "")
            Files.write(path, bytes!!)
            bot.addStickerToSet(chatId, entity.animatedPackName, path.toFile(), sticker.emoji!!, sticker.maskPosition, StickerType.ANIMATED)
            Files.delete(path)
            bot.sendMessage(
                    chatId,
                    "Successfully added :)",
                    replyToMessageId = update.message!!.messageId,
                    replyMarkup = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton("Sticker pack", "https://t.me/addstickers/${entity.animatedPackName}"))))
            ).body()
        } else {
            val containsMask = sticker.maskPosition != null
            val bytes = bot.downloadFileBytes(sticker.fileId)
            val path = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", "")
            Files.write(path, bytes!!)
            bot.createNewAnimatedStickerSet(chatId, entity.animatedPackName, "Your animated stickers - @my_stckrs_bot", path.toFile(), sticker.emoji!!, containsMask, sticker.maskPosition)
            Files.delete(path)
            dao.setCreatedStatus(chatId, animatedStickerCreated = true)
            bot.sendMessage(
                    chatId,
                    "Successfully created sticker pack and added this sticker to it :)",
                    replyToMessageId = update.message!!.messageId,
                    replyMarkup = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton("Sticker pack", "https://t.me/addstickers/${entity.animatedPackName}"))))
            ).body()
        }
    }.unit()

}
