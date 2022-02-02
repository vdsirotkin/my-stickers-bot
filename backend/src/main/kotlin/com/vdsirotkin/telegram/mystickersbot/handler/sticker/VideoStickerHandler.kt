package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class VideoStickerHandler(
    override val stickerDao: StickerDAO,
    override val messageSource: MessageSource,
    private val fileHelper: FileHelper,
    private val stickerPackManagementService: StickerPackManagementService,
    private val stickerPackMessagesSender: StickerPackMessagesSender,
    private val botConfigProps: BotConfigProps
): LocalizedHandler {

    override fun handleInternal(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper, entity: UserEntity): Mono<BaseHandler> = mdcMono {
        val fileId = update.message().sticker().fileId()
        val chatId = update.message().chat().id()

        val webmFile = fileHelper.downloadFile(bot, fileId)
        if (entity.videoPackCreated) {
            stickerPackManagementService.video().addStickerToPack(bot, chatId, webmFile, entity, update.message().sticker().emoji())
            stickerPackMessagesSender.video().sendSuccessAdd(bot, chatId, messageSource, update.message().messageId(), entity, action)
        } else {
            val resultEntity = updateVideoPackNameIfNesessary(entity)
            stickerPackManagementService.video().createNewPack(bot, chatId, webmFile, resultEntity, update.message().sticker().emoji())
            stickerPackMessagesSender.video().sendSuccessCreated(bot, chatId, messageSource, update.message().messageId(), resultEntity, action)
            stickerDao.setCreatedStatus(chatId, videoPackCreated = true)
        }
        stickerDao.saveVideoSticker(chatId, StickerMeta(fileId, update.message().sticker().fileUniqueId(), update.message().sticker().emoji()))
    }.thenReturn(this)

    private suspend fun updateVideoPackNameIfNesessary(entity: UserEntity): UserEntity {
        if (entity.videoPackName.isNotEmpty()) return entity
        val vidPackName = "vid_stckr_${entity.userId}_by_${botConfigProps.username}"
        return stickerDao.saveVideoPackName(entity.userId, vidPackName)
    }

    override val action: String
        get() = "VIDEO_STICKER"
}