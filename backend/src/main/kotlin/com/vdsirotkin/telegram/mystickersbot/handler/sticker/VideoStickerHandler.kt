package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import com.vdsirotkin.telegram.mystickersbot.dto.packType
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.PackNameProvider
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

// ffmpeg -i 1475461161146.gif.mp4 -c:v libvpx-vp9 -c:a libopus -vf scale=512:384 -to 00:00:03 1475461161146.gif.webm

@Service
class VideoStickerHandler(
    override val stickerDao: StickerDAO,
    override val messageSource: MessageSource,
    private val fileHelper: FileHelper,
    private val stickerPackManagementService: StickerPackManagementService,
    private val stickerPackMessagesSender: StickerPackMessagesSender,
    private val packNameProvider: PackNameProvider,
): LocalizedHandler {

    override fun handleInternal(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper, entity: UserEntity): Mono<BaseHandler> = mdcMono {
        val sticker = update.message().sticker()
        val fileId = sticker.fileId()
        val chatId = update.message().chat().id()

        if (stickerDao.stickerExists(entity, sticker)) {
            bot.executeAsync(SendMessageWithAction(chatId, messageSource["sticker.already.added"], action).replyToMessageId(update.message().messageId()))
            return@mdcMono
        }
        if (entity.videoPackCreated) {
            stickerPackManagementService.video().addStickerToPack(bot, chatId, sticker.fileId(), entity, sticker.emoji())
            stickerPackMessagesSender.video().sendSuccessAdd(bot, chatId, messageSource, update.message().messageId(), entity, action)
        } else {
            val resultEntity = updateVideoPackNameIfNecessary(entity)
            stickerPackManagementService.video().createNewPack(bot, chatId, sticker.fileId(), resultEntity, sticker.emoji())
            stickerPackMessagesSender.video().sendSuccessCreated(bot, chatId, messageSource, update.message().messageId(), resultEntity, action)
            stickerDao.createSet(chatId, StickerPackType.VIDEO, entity.videoPackName)
        }
        stickerDao.saveSticker(chatId, StickerMeta(fileId, sticker.fileUniqueId(), sticker.emoji(), sticker.packType()), createdStickerUniqueFileId = "")
    }.thenReturn(this)

    private suspend fun updateVideoPackNameIfNecessary(entity: UserEntity): UserEntity {
        if (entity.videoPackName.isNotEmpty()) return entity
        val vidPackName = with(packNameProvider) { StickerPackType.VIDEO(entity.userId.toLong(), 0) }
        return stickerDao.saveVideoPackName(entity.userId, vidPackName)
    }

    override val action: String
        get() = "VIDEO_STICKER"
}
