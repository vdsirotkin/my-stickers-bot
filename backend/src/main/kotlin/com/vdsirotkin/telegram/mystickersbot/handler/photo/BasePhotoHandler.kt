package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import org.telegram.telegrambots.bots.DefaultAbsSender

interface BasePhotoHandler : LocalizedHandler {

    val stickerPackMessagesSender: StickerPackMessagesSender
    val stickerPackManagementService: StickerPackManagementService
    val imageResizer: ImageResizer
    val fileHelper: FileHelper

    suspend fun processPhoto(bot: DefaultAbsSender, fileId: String,
                             entity: UserEntity,
                             chatId: Long,
                             messageId: Int,
                             messageSourceWrapper: MessageSourceWrapper) {
        val file = fileHelper.downloadFile(bot, fileId)
        val resized = withTempFile(file) {
            imageResizer.resizeImage(file)
        }
        withTempFile(resized) {
            if (entity.normalPackCreated) {
                stickerPackManagementService.addStickerToPack(bot, chatId, resized, entity)
                stickerPackMessagesSender.sendSuccessAdd(bot, chatId, messageSourceWrapper, messageId, entity)
            } else {
                stickerPackManagementService.createNewPack(bot, chatId, resized, entity)
                stickerDao.setCreatedStatus(chatId, normalStickerCreated = true)
                stickerPackMessagesSender.sendSuccessCreated(bot, chatId, messageSourceWrapper, messageId, entity)
            }
        }
    }

}
