package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.pengrad.telegrambot.model.Update
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.sokomishalov.commons.core.log.Loggable

@Component
@Scope(SCOPE_PROTOTYPE)
class PhotoHandler(override val stickerPackMessagesSender: StickerPackMessagesSender,
                   override val stickerPackManagementService: StickerPackManagementService,
                   override val imageResizer: ImageResizer,
                   override val fileHelper: FileHelper,
                   override val stickerDao: StickerDAO,
                   override val messageSource: MessageSource
) : BasePhotoHandler() {

    override fun canBeProcessed(update: Update): Boolean = true

    override fun getFileId(update: Update): String {
        val photos = update.message().photo()
        logger.info("Received photos: $photos")
        val photo = photos.maxByOrNull { it.fileSize() }!!
        logger.info("Selected photo: $photo")
        return photo.fileId()
    }

    override val action: String
        get() = "PHOTO"

    companion object : Loggable


}


