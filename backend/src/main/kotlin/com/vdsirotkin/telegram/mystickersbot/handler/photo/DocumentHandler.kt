package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class DocumentHandler(override val stickerPackMessagesSender: StickerPackMessagesSender,
                      override val stickerPackManagementService: StickerPackManagementService,
                      override val imageResizer: ImageResizer,
                      override val fileHelper: FileHelper,
                      override val stickerDao: StickerDAO,
                      override val messageSource: MessageSource
) : BasePhotoHandler() {


    private val supportedTypes: Array<String> = arrayOf("image/jpeg", "image/png")

    override fun canBeProcessed(update: Update): Boolean {
        val document = update.message.document
        return document.mimeType in supportedTypes
    }

    override fun getFileId(update: Update): String = update.message.document.fileId


}
