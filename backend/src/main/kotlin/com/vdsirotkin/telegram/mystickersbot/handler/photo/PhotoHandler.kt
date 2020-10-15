package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.sokomishalov.commons.core.log.Loggable

@Component
class PhotoHandler(override val stickerPackMessagesSender: StickerPackMessagesSender,
                   override val stickerPackManagementService: StickerPackManagementService,
                   override val imageResizer: ImageResizer,
                   override val fileHelper: FileHelper,
                   override val stickerDao: StickerDAO,
                   override val messageSource: MessageSource
) : BasePhotoHandler() {

    override fun canBeProcessed(update: Update): Boolean = true

    override fun getFileId(update: Update): String {
        val photos = update.message.photo
        logger.info("Received photos: $photos")
        val photo = photos.maxByOrNull { it.fileSize }!!
        logger.info("Selected photo: $photo")
        return photo.fileId
    }

    companion object : Loggable


}


