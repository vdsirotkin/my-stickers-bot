package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.dao.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.addInlineKeyboard
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.monoWithMdc
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import io.github.biezhi.webp.WebpIO
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
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt

@Service
class NormalStickerHandler(
        private val dao: StickerDAO,
        private val props: BotConfigProps
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> {
        val chatId = update.message!!.chat.id
        return monoWithMdc {
            val entity = dao.getUserEntity(chatId)
            if (entity.normalPackCreated) {
                addStickerToPack(bot, update, entity)
            } else {
                createNewPack(bot, entity, update)
            }
        }
    }

    private suspend fun createNewPack(bot: DefaultAbsSender,
                                      entity: UserEntity,
                                      update: Update) {
        val sticker = update.message!!.sticker!!
        val chatId = update.message!!.chat.id
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
                .setReplyToMessageId(update.message!!.messageId)
                .addInlineKeyboard("Sticker pack", "https://t.me/addstickers/${entity.normalPackName}"))
    }

    private suspend fun addStickerToPack(bot: DefaultAbsSender,
                                         update: Update,
                                         entity: UserEntity) {
        val sticker = update.message!!.sticker!!
        val chatId = update.message!!.chat.id
        withTempFile(preparePngFile(bot, sticker)) {
            bot.execute(AddStickerToSet(chatId.toInt(), entity.normalPackName, sticker.emoji!!).setPngSticker(it))
        }
        bot.executeAsync(SendMessage(chatId, "This sticker added!")
                .setReplyToMessageId(update.message!!.messageId)
                .addInlineKeyboard("Sticker pack", "https://t.me/addstickers/${entity.normalPackName}")
        )
    }

    private suspend fun preparePngFile(bot: DefaultAbsSender,
                                       sticker: Sticker): File {
        val file = withContext(Dispatchers.IO) { Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".webp").toFile() }
        return withTempFile(file) { webpFile ->
            val stickerFile = bot.executeAsync(GetFile().setFileId(sticker.fileId))
            bot.downloadFile(stickerFile, webpFile)
            val pngFilePath = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".png")
            val pngFile = pngFilePath.toFile()
            WebpIO.create().toNormalImage(webpFile, pngFile)
            if ((Files.size(pngFilePath) / 1024) > 500) {
                reducePngSize(webpFile, pngFilePath, sticker.width, sticker.height)
            }
            webpFile.delete()
            pngFile
        }
    }

    private fun reducePngSize(webpFile: File, pngFilePath: Path, width: Int, height: Int) {
        val newWidth = (width * 0.95).roundToInt()
        val newHeight = (height * 0.95).roundToInt()
        WebpIO.create().toNormalImage(webpFile, pngFilePath.toFile(), "-scale $newWidth $newHeight")
        if ((Files.size(pngFilePath) / 1024) > 500) {
            reducePngSize(webpFile, pngFilePath, newWidth, newHeight)
        }
    }
}
