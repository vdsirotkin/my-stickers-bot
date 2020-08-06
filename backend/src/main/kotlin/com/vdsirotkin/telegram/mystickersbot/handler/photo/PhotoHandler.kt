package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

@Component
class PhotoHandler(override val stickerPackMessagesSender: StickerPackMessagesSender,
                   override val stickerPackManagementService: StickerPackManagementService,
                   override val imageResizer: ImageResizer,
                   override val fileHelper: FileHelper,
                   override val stickerDao: StickerDAO,
                   override val messageSource: MessageSource
) : BasePhotoHandler {

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
        processPhoto(bot, photo.fileId, entity, chatId, update.message.messageId, messageSource)
    }.thenReturn(Unit)

    companion object : Loggable

    data class Dimensions(val width: Int, val height: Int)


}
