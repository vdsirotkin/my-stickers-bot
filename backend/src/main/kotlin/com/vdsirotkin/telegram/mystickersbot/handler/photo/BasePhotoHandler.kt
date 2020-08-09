package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

interface BasePhotoHandler : LocalizedHandler {

    val stickerPackMessagesSender: StickerPackMessagesSender
    val stickerPackManagementService: StickerPackManagementService
    val imageResizer: ImageResizer
    val fileHelper: FileHelper

    suspend fun processPhoto(bot: DefaultAbsSender, fileId: String,
                             entity: UserEntity,
                             chatId: Long,
                             messageId: Int,
                             messageSource: MessageSourceWrapper) {
        val file = fileHelper.downloadFile(bot, fileId)
        val resized = try {
            withTempFile(file) {
                imageResizer.resizeImage(file)
            }
        } catch (e: PngNotCreatedException) {
            bot.executeAsync(SendMessage(chatId, messageSource.getMessage("photo.cant.be.processed")))
            return
        }
        withTempFile(resized) {
            if (entity.normalPackCreated) {
                stickerPackManagementService.addStickerToPack(bot, chatId, resized, entity)
                stickerPackMessagesSender.sendSuccessAdd(bot, chatId, messageSource, messageId, entity)
            } else {
                stickerPackManagementService.createNewPack(bot, chatId, resized, entity)
                stickerDao.setCreatedStatus(chatId, normalStickerCreated = true)
                stickerPackMessagesSender.sendSuccessCreated(bot, chatId, messageSource, messageId, entity)
            }
        }
    }

}
