package com.vdsirotkin.telegram.mystickersbot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.stickers.Sticker
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.dao.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.body
import io.github.biezhi.webp.WebpIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import ru.sokomishalov.commons.core.common.unit
import java.io.File
import java.nio.file.Files

@Service
class NormalStickerHandler(
        private val dao: StickerDAO
) : BaseHandler {

    override fun handle(bot: Bot, update: Update) = runBlocking(Dispatchers.IO) {
        val chatId = update.message!!.chat.id
        val sticker = update.message!!.sticker!!
        val entity = dao.getUserEntity(chatId)
        if (entity.normalPackCreated) {
            addStickerToPack(bot, update, entity)
        } else {
            val containsMask = sticker.maskPosition != null
            val png = preparePngFile(bot, sticker)
            bot.createNewStickerSet(chatId, entity.normalPackName, "Your stickers - @my_stckrs_bot", png, sticker.emoji!!, containsMask, sticker.maskPosition).body()
            png.delete()
            dao.setCreatedStatus(chatId, normalStickerCreated = true)
            bot.sendMessage(
                    chatId,
                    "Successfully created sticker pack and added this sticker to it :)",
                    replyToMessageId = update.message!!.messageId,
                    replyMarkup = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton("Sticker pack", "https://t.me/addstickers/${entity.normalPackName}"))))
            ).body()
        }
    }.unit()

    private fun preparePngFile(bot: Bot,
                               sticker: Sticker): File {
        val bytes = bot.downloadFileBytes(sticker.fileId)
        val webpPath = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".webp")
        Files.write(webpPath, bytes!!)
        val pngFile = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".png").toFile()
        WebpIO.create().toNormalImage(webpPath.toFile(), pngFile)
        Files.delete(webpPath)
        return pngFile
    }

    private fun addStickerToPack(bot: Bot,
                                 update: Update,
                                 entity: UserEntity) {
        val sticker = update.message!!.sticker!!
        val chatId = update.message!!.chat.id
        val png = preparePngFile(bot, sticker)
        bot.addStickerToSet(chatId, entity.normalPackName, png, sticker.emoji!!, sticker.maskPosition).body()
        png.delete()
        bot.sendMessage(chatId, "This sticker added!", replyToMessageId = update.message!!.messageId,
                replyMarkup = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton("Sticker pack", "https://t.me/addstickers/${entity.normalPackName}"))))).body()
    }

}
