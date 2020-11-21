package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.EditMessageReplyMarkup
import com.pengrad.telegrambot.request.SendMessage
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
import com.vdsirotkin.telegram.mystickersbot.util.*
import com.vdurmont.emoji.EmojiParser
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

abstract class BasePhotoHandler : LocalizedHandler, StatefulHandler<PhotoHandlerData> {

    abstract val stickerPackMessagesSender: StickerPackMessagesSender
    abstract val stickerPackManagementService: StickerPackManagementService
    abstract val imageResizer: ImageResizer
    abstract val fileHelper: FileHelper


    abstract fun canBeProcessed(update: Update): Boolean

    abstract fun getFileId(update: Update): String

    private var photoHandlerState = PhotoHandlerState(false, PhotoHandlerData(NEW))

    override fun handleInternal(
            bot: TelegramBot,
            update: Update,
            messageSource: MessageSourceWrapper,
            userEntity: UserEntity
    ): Mono<BaseHandler> = statefulMdcMono {
        runStateMachine(bot, update, messageSource, userEntity)
    }

    private suspend fun runStateMachine(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper, user: UserEntity) {
        val chatId = determineChatId(update)
        val data = photoHandlerState.data
        when (data.state) {
            NEW -> {
                if (!canBeProcessed(update)) {
                    bot.executeAsync(SendMessage(chatId, messageSource["document.not.supported"]))
                    photoHandlerState = photoHandlerState.toFinished()
                    return
                }
                val message = bot.executeAsync(SendMessage(chatId, messageSource["emoji.required"]).addEmojiKeyboard())
                val fileId = getFileId(update)
                val meta = PhotoMeta(fileId, update.message().messageId(), message.message().messageId())
                photoHandlerState = photoHandlerState.toWaitingForEmoji(meta)
            }
            WAITING_FOR_EMOJI -> {
                requireNotNull(data.photoMeta)
                when {
                    update.message() != null && update.message().text() != null -> {
                        val emojis = EmojiParser.extractEmojis(update.message().text())
                        if (emojis.isEmpty()) {
                            bot.executeAsync(SendMessage(chatId, messageSource["send.emojis.message"]))
                            return
                        }
                        val emojiStr = emojis.joinToString()
                        processPhoto(bot, data.photoMeta.fileId, user, chatId, data.photoMeta.messageId, messageSource, emojiStr)
                        photoHandlerState = photoHandlerState.toFinished()
                    }
                    update.callbackQuery() != null -> {
                        val emojiStr = parseEmoji(update.callbackQuery().data())
                        bot.executeAsync(EditMessageReplyMarkup(chatId, data.photoMeta.emojiMessageId).replyMarkup(InlineKeyboardMarkup()))
                        processPhoto(bot, data.photoMeta.fileId, user, chatId, data.photoMeta.messageId, messageSource, emojiStr)
                        photoHandlerState = photoHandlerState.toFinished()
                    }
                    else -> {
                        bot.executeAsync(SendMessage(chatId, messageSource["send.emojis.message"]))
                    }
                }
            }
        }
    }

    private suspend fun processPhoto(
            bot: TelegramBot, fileId: String,
            entity: UserEntity,
            chatId: Long,
            messageId: Int,
            messageSource: MessageSourceWrapper,
            emoji: String? = null,
    ) {
        bot.executeAsync(SendMessage(chatId, messageSource["please.wait"]))
        val file = fileHelper.downloadFile(bot, fileId)
        val resized = try {
            withTempFile(file) {
                imageResizer.resizeImage(file)
            }
        } catch (e: PngNotCreatedException) {
            bot.executeAsync(SendMessage(chatId, messageSource["photo.cant.be.processed"]))
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

    data class PhotoMeta(val fileId: String, val messageId: Int, val emojiMessageId: Int)

    enum class State {
        NEW, WAITING_FOR_EMOJI
    }

    private fun PhotoHandlerState.toWaitingForEmoji(meta: PhotoMeta): PhotoHandlerState {
        return this.copy(data = data.copy(photoMeta = meta, state = WAITING_FOR_EMOJI))
    }

    private fun PhotoHandlerState.toFinished(): PhotoHandlerState {
        return this.copy(finished = true)
    }

    companion object : Loggable

    override suspend fun cancel(bot: TelegramBot, chatId: Long) {
        val messageId = photoHandlerState.data.photoMeta?.emojiMessageId
        if (messageId != null) {
           bot.executeAsync(EditMessageReplyMarkup(chatId, messageId).replyMarkup(InlineKeyboardMarkup()))
        }
    }
}
