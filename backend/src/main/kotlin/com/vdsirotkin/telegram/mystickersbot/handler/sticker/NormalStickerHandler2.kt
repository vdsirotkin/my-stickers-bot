package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.tinder.StateMachine
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.PngService
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import com.vdurmont.emoji.EmojiParser
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class NormalStickerHandler2(
        private val pngService: PngService,
        private val stickerPackManagementService: StickerPackManagementService,
        private val stickerPackMessagesSender: StickerPackMessagesSender,
        override val stickerDao: StickerDAO,
        override val messageSource: MessageSource
) : LocalizedHandler {

    val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.New)
        state<State.New> {
            on<Event.ReceivedMessage> { runBlocking {
                val (bot, update, messageSource, entity) = it.toEventDC()
                val chatId = update.message!!.chatId
                val sticker = update.message!!.sticker!!
                if (stickerDao.stickerExists(entity, sticker, false)) {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.already.added")).setReplyToMessageId(update.message!!.messageId))
                    transitionTo(State.Finished)
                }
                logDebug(sticker.toString())
                if (sticker.emoji == null) {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("emoji.required")))
                    transitionTo(State.WaitingForEmoji)
                } else {
                    transitionTo(State.AllDone)
                }
            } }
        }
        state<State.WaitingForEmoji> {
            on<Event.ReceivedMessage> {
                if (update.message.hasText()) {
                    val emojis = EmojiParser.extractEmojis(update.message.text)
                    if (emojis.isEmpty()) {
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                        return
                    }
                    val emojiStr = emojis.joinToString()
                    state = state.toAllDone(state.data.stickerMeta!!.copy(emoji = emojiStr))
                    runStateMachine(chatId, update, bot, messageSource, entity)
                } else {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                }
            }
        }
    }


    override fun handleInternal(bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper,
                                userEntity: UserEntity): Mono<BaseHandler> = mdcMono {

    }.thenReturn(this)

    sealed class State {
        object New: State()
        object Started: State()
        object WaitingForEmoji: State()
        object AllDone: State()
        object Finished: State()
    }

    sealed class Event(
            val bot: DefaultAbsSender,
            val update: Update,
            val messageSource: MessageSourceWrapper,
            val userEntity: UserEntity
    ) {

        class ReceivedMessage(bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper, userEntity: UserEntity) : Event(bot, update, messageSource, userEntity)

        fun toEventDC(): EventDC = EventDC(bot, update, messageSource, userEntity)

    }

    data class EventDC(val bot: DefaultAbsSender,
                       val update: Update,
                       val messageSource: MessageSourceWrapper,
                       val userEntity: UserEntity)

    sealed class SideEffect

    companion object: Loggable


}

