package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.handler.photo.BasePhotoHandler.PhotoHandlerData
import com.vdsirotkin.telegram.mystickersbot.handler.photo.BasePhotoHandler.State.NEW
import com.vdsirotkin.telegram.mystickersbot.handler.photo.BasePhotoHandler.State.WAITING_FOR_EMOJI
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import com.vdurmont.emoji.EmojiParser
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

abstract class BasePhotoHandler : LocalizedHandler, StatefulHandler<PhotoHandlerData> {

    abstract val stickerPackMessagesSender: StickerPackMessagesSender
    abstract val stickerPackManagementService: StickerPackManagementService
    abstract val imageResizer: ImageResizer
    abstract val fileHelper: FileHelper

    private var photoHandlerState = PhotoHandlerState(false, PhotoHandlerData(NEW))

    override fun handleInternal(bot: DefaultAbsSender,
                                update: Update,
                                messageSource: MessageSourceWrapper): Mono<BaseHandler> = mdcMono {

        runStateMachine(bot, update, messageSource)
    }.thenReturn(this)

    private suspend fun runStateMachine(
            bot: DefaultAbsSender,
            update: Update,
            messageSource: MessageSourceWrapper,
    ) {
        val chatId = update.message.chatId
        val data = photoHandlerState.data
        when (data.state) {
            NEW -> {
                if (!canBeProcessed(update)) {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("document.not.supported")))
                    photoHandlerState = photoHandlerState.toFinished()
                    return
                }
                val fileId = getFileId(update)
                val meta = PhotoMeta(fileId, update.message.messageId)
                photoHandlerState = photoHandlerState.toWaitingForEmoji(meta)
                bot.executeAsync(SendMessage(chatId, messageSource.getMessage("emoji.required")))
            }
            WAITING_FOR_EMOJI -> {
                if (update.message.hasText()) {
                    val emojis = EmojiParser.extractEmojis(update.message.text)
                    if (emojis.isEmpty()) {
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                        return
                    }
                    val emojiStr = emojis.joinToString()
                    val entity = stickerDao.getUserEntity(chatId)
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("please.wait")))
                    processPhoto(bot, data.photoMeta!!.fileId, entity, chatId, data.photoMeta.messageId, messageSource, emojiStr)
                    photoHandlerState = photoHandlerState.toFinished()
                } else {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                }
            }
        }
    }

    companion object : Loggable

    override fun getState(): HandlerState<PhotoHandlerData> = photoHandlerState

    override fun setState(state: HandlerState<PhotoHandlerData>) {
        photoHandlerState = state as PhotoHandlerState
    }

    data class PhotoHandlerData(val state: State, val photoMeta: PhotoMeta? = null)

    data class PhotoHandlerState(
            override val finished: Boolean,
            override val data: PhotoHandlerData,
            override val handlerClass: String = PhotoHandler::class.java.name
    ) : HandlerState<PhotoHandlerData>

    data class PhotoMeta(val fileId: String, val messageId: Int)

    enum class State {
        NEW, WAITING_FOR_EMOJI
    }

    private fun PhotoHandlerState.toWaitingForEmoji(meta: PhotoMeta): PhotoHandlerState {
        return this.copy(data = data.copy(photoMeta = meta, state = WAITING_FOR_EMOJI))
    }

    private fun PhotoHandlerState.toFinished(): PhotoHandlerState {
        return this.copy(finished = true)
    }

    private suspend fun processPhoto(
            bot: DefaultAbsSender, fileId: String,
            entity: UserEntity,
            chatId: Long,
            messageId: Int,
            messageSource: MessageSourceWrapper,
            emoji: String? = null,
    ) {
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
                stickerPackManagementService.addStickerToPack(bot, chatId, resized, entity, emoji)
                stickerPackMessagesSender.sendSuccessAdd(bot, chatId, messageSource, messageId, entity)
            } else {
                stickerPackManagementService.createNewPack(bot, chatId, resized, entity, emoji)
                stickerDao.setCreatedStatus(chatId, normalStickerCreated = true)
                stickerPackMessagesSender.sendSuccessCreated(bot, chatId, messageSource, messageId, entity)
            }
        }
    }

    abstract fun canBeProcessed(update: Update): Boolean

    abstract fun getFileId(update: Update): String

}
