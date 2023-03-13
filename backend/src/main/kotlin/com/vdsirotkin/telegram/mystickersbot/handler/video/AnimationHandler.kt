package com.vdsirotkin.telegram.mystickersbot.handler.video

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.EditMessageReplyMarkup
import com.pengrad.telegrambot.request.SendMessage
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.video.VideoService
import com.vdsirotkin.telegram.mystickersbot.util.*
import com.vdurmont.emoji.EmojiParser
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class AnimationHandler(
    override val stickerDao: StickerDAO,
    override val messageSource: MessageSource,
    private val videoService: VideoService,
    private val fileHelper: FileHelper,
    private val stickerPackManagementService: StickerPackManagementService,
    private val stickerPackMessagesSender: StickerPackMessagesSender,
    private val packNameProvider: PackNameProvider,
): LocalizedHandler, StatefulHandler<AnimationHandler.State> {

    private val supportedTypes = arrayOf("video/mp4")

    var state = AnimationStickerHandlerState(State.New)

    var stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.New)
        state<State.New> {
            on<Event.MessageReceivedEvent> {
                val (bot, update, messageSourceWrapper, _) = it
                val chatId = determineChatId(update)
                if (!supportedTypes.contains(update.message().animation().mimeType())) {
                    bot.executeAsync(SendMessage(update.message().chat().id(), messageSourceWrapper["video.not.supported"]))
                    return@on transitionTo(State.Finished)
                }
                val message = bot.executeAsync(SendMessageWithAction(chatId, messageSourceWrapper["emoji.required"], action).addEmojiKeyboard())
                transitionTo(State.WaitingForEmoji(update.message(), message.message().messageId()))
            }
        }
        state<State.WaitingForEmoji> {
            on<Event.MessageReceivedEvent> {
                val (bot, update, messageSourceWrapper, user) = it
                val chatId = determineChatId(update)
                when {
                    update.message() != null && update.message().text() != null -> {
                        val emojis = EmojiParser.extractEmojis(update.message().text())
                        if (emojis.isEmpty()) {
                            bot.executeAsync(SendMessageWithAction(chatId, messageSourceWrapper["send.emojis.message"], action))
                            return@on dontTransition()
                        }
                        val emojiStr = emojis.joinToString()
                        processVideo(bot, user, this.message, messageSourceWrapper, emojiStr)
                        transitionTo(State.Finished)
                    }
                    update.callbackQuery() != null -> {
                        val emojiStr = parseEmoji(update.callbackQuery().data())
                        bot.executeAsync(EditMessageReplyMarkup(chatId, this.emojiMessageId).replyMarkup(InlineKeyboardMarkup()))
                        processVideo(bot, user, this.message, messageSourceWrapper, emojiStr)
                        transitionTo(State.Finished)
                    }
                    else -> {
                        bot.executeAsync(SendMessageWithAction(chatId, messageSourceWrapper["send.emojis.message"], action))
                        dontTransition()
                    }
                }
            }
        }
        state<State.Finished> {}
    }

    private suspend fun processVideo(bot: TelegramBot, user: UserEntity, message: Message, messageSourceWrapper: MessageSourceWrapper, emojiStr: String) {
        bot.executeAsync(SendMessageWithAction(user.userId, messageSourceWrapper["please.wait"], action))
        val mp4File = fileHelper.downloadFile(bot, message.animation().fileId())
        withTempFile(videoService.convertToWebm(message.animation(), mp4File)) { webmFile ->
            if (user.videoPackCreated) {
                stickerPackManagementService.video().addStickerToPack(bot, user.userId.toLong(), webmFile, user, emojiStr)
                stickerPackMessagesSender.video().sendSuccessAdd(bot, user.userId.toLong(), messageSourceWrapper, message.messageId(), user, action)
            } else {
                val resultUser = updateVideoPackNameIfNesessary(user)
                stickerPackManagementService.video().createNewPack(bot, user.userId.toLong(), webmFile, resultUser, emojiStr)
                stickerDao.createSet(user.userId.toLong(), StickerPackType.VIDEO, user.videoPackName)
                stickerPackMessagesSender.video().sendSuccessCreated(bot, user.userId.toLong(), messageSourceWrapper, message.messageId(), resultUser, action)
            }
        }
    }

    private suspend fun updateVideoPackNameIfNesessary(entity: UserEntity): UserEntity {
        if (entity.videoPackName.isNotEmpty()) return entity
        val vidPackName = with(packNameProvider) { StickerPackType.VIDEO(entity.userId.toLong(), 0) }
        return stickerDao.saveVideoPackName(entity.userId, vidPackName)
    }

    sealed class State {
        object New : State()
        class WaitingForEmoji(val message: Message, val emojiMessageId: Int) : State()
        object Finished : State()
    }

    sealed class Event {
        class MessageReceivedEvent(val bot: TelegramBot, val update: Update, val messageSource: MessageSourceWrapper, val userEntity: UserEntity) : Event() {
            operator fun component1(): TelegramBot = bot
            operator fun component2(): Update = update
            operator fun component3(): MessageSourceWrapper = messageSource
            operator fun component4(): UserEntity = userEntity
        }
    }

    sealed class SideEffect

    data class AnimationStickerHandlerState(
        override val data: State,
        override val handlerClass: String = AnimationHandler::class.java.name,
    ) : HandlerState<State> {
        override val finished: Boolean
            get() = data == State.Finished
    }

    override fun handleInternal(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper, userEntity: UserEntity): Mono<BaseHandler> = statefulMdcMono {
        stateMachine = stateMachine.with { initialState(state.data) }
        stateMachine.transition(Event.MessageReceivedEvent(bot, update, messageSource, userEntity))
        state = state.copy(data = stateMachine.state)
    }

    override val action: String
        get() = "ANIMATION_TO_VIDEO_STICKER"

    override fun getState(): HandlerState<State> = state

    override fun setState(state: HandlerState<State>) {
        this.state = state as AnimationStickerHandlerState
    }
}
