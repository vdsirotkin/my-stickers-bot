package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.imaging.Imaging
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.stickers.AddStickerToSet
import org.telegram.telegrambots.meta.api.objects.PhotoSize
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable
import java.io.File
import java.nio.file.Files
import kotlin.math.roundToInt

@Component
class PhotoHandler(override val stickerDao: StickerDAO, override val messageSource: MessageSource) : LocalizedHandler {

    override fun handleInternal(bot: DefaultAbsSender,
                                update: Update,
                                messageSource: MessageSourceWrapper): Mono<Unit> = mdcMono {
        val chatId = update.message.chatId
        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("please.wait")))
        val entity = stickerDao.getUserEntity(chatId)
        val photos = update.message.photo
        logger.info("Received photos: $photos")
        val photo = photos.maxBy { it.fileSize }!!
        logger.info("Selected photo: $photo")
        val file = downloadFile(bot, photo)
        val resized = withTempFile(file) {
            resizeImage(file)
        }
        withTempFile(resized) {
            bot.execute(AddStickerToSet(chatId.toInt(), entity.normalPackName, "😀").setPngSticker(resized))
            bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.added"))
                    .setReplyToMessageId(update.message.messageId)
                    .addInlineKeyboard(messageSource.getMessage("sticker.pack.button.text"), "https://t.me/addstickers/${entity.normalPackName}"))
        }
        return@mdcMono
    }

    private fun resizeImage(file: File): File {
        val metadata = Imaging.getImageInfo(file)
        val width = metadata.width
        val height = metadata.height
        val newImage = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".png").toFile()
        val dimensions = when {
            height > width -> { // vertical
                val newHeight = 512
                val coefficient = newHeight.toDouble() / height.toDouble()
                val newWidth = (width * coefficient).roundToInt()
                Dimensions(newWidth, newHeight)
            }
            width > height -> { // horizontal
                val newWidth = 512
                val coefficient = newWidth.toDouble() / width.toDouble()
                val newHeight = (height * coefficient).roundToInt()
                Dimensions(newWidth, newHeight)
            }
            else -> { // square
                val newWidth = 512
                val newHeight = 512
                Dimensions(newWidth, newHeight)
            }
        }
        val (newWidth, newHeight) = dimensions
        logger.info("Old dimensions: w$width,h$height, new dimesions: w$newWidth,h$newHeight")
        Thumbnails.of(file).forceSize(newWidth, newHeight).toFile(newImage)
        return newImage
    }

    private suspend fun downloadFile(bot: DefaultAbsSender, photoSize: PhotoSize): File = withContext(Dispatchers.IO) {
        val file = bot.executeAsync(GetFile().apply { fileId = photoSize.fileId })
        bot.downloadFile(file.filePath)
    }

    companion object : Loggable

    data class Dimensions(val width: Int, val height: Int)


}
