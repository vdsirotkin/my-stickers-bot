package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.tinder.StateMachine
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackManagementService
import com.vdsirotkin.telegram.mystickersbot.service.StickerPackMessagesSender
import com.vdsirotkin.telegram.mystickersbot.service.image.PngService
import com.vdsirotkin.telegram.mystickersbot.util.*
import com.vdurmont.emoji.EmojiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable
import java.io.File
import java.nio.file.Files

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class NormalStickerHandler2(
        private val pngService: PngService,
        private val stickerPackManagementService: StickerPackManagementService,
        private val stickerPackMessagesSender: StickerPackMessagesSender,
        override val stickerDao: StickerDAO,
        override val messageSource: MessageSource,
) : LocalizedHandler, StatefulHandler<NormalStickerHandler2.NormalStickerHandlerStateData> {

    private var state = NormalStickerHandlerState(NormalStickerHandlerStateData(State.New))

    var stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.New)
        state<State.New> {
            on<Event.ReceivedMessage> {
                runBlocking {
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
                        transitionTo(State.WaitingForEmoji, SideEffect.SaveMinimumMeta(sticker.fileId, sticker.fileUniqueId))
                    } else {
                        transitionTo(State.AllDone, SideEffect.SaveFullMeta(sticker.fileId, sticker.fileUniqueId, sticker.emoji))
                    }
                }
            }
        }
        state<State.WaitingForEmoji> {
            on<Event.ReceivedMessage> {
                runBlocking {
                    val (bot, update, messageSource, entity) = it.toEventDC()
                    val chatId = update.message!!.chatId
                    if (update.message.hasText()) {
                        val emojis = EmojiParser.extractEmojis(update.message.text)
                        if (emojis.isEmpty()) {
                            bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                            dontTransition()
                        }
                        val emojiStr = emojis.joinToString()
                        transitionTo(State.AllDone, SideEffect.SaveEmoji(emojiStr))
                    } else {
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                        dontTransition()
                    }
                }
            }
        }
        state<State.AllDone> {
            onEnter {
                runBlocking {
                    val (bot, update, messageSource, entity) = it.toEventDC()
                    val chatId = update.message!!.chatId
                    val messageId = update.message!!.messageId
                    try {
                        processSticker(bot, state.data.meta!!, entity, chatId, messageSource, messageId)
                    } catch (e: PngNotCreatedException) {
                        logger.warn("Can't create png from this sticker")
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.cant.be.processed")).setReplyToMessageId(messageId))
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        transitionTo(State.Finished)
                    }
                }
            }
        }
        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
            when (val effect = validTransition.sideEffect) {
                is SideEffect.SaveMinimumMeta -> state = state.copy(data = state.data.copy(meta = StickerMeta(effect.fileId, effect.fileUniqueId)))
                is SideEffect.SaveEmoji -> state = state.copy(data = state.data.copy(meta = state.data.meta!!.copy(emoji = effect.emojiStr)))
                is SideEffect.SaveFullMeta -> state = state.copy(data = state.data.copy(meta = StickerMeta(effect.fileId, effect.fileUniqueId, effect.emojiStr)))
            }
        }
    }

    private suspend fun processSticker(
            bot: DefaultAbsSender,
            sticker: StickerMeta,
            entity: UserEntity,
            chatId: Long,
            messageSource: MessageSourceWrapper,
            messageId: Int,
    ) {
        withTempFile(preparePngFile(bot, sticker)) {
            if (entity.normalPackCreated) {
                stickerPackManagementService.addStickerToPack(bot, chatId, it, entity, sticker.emoji)
                stickerPackMessagesSender.sendSuccessAdd(bot, chatId, messageSource, messageId, entity)
            } else {
                stickerPackManagementService.createNewPack(bot, chatId, it, entity, sticker.emoji)
                stickerDao.setCreatedStatus(chatId, normalStickerCreated = true)
                stickerPackMessagesSender.sendSuccessCreated(bot, chatId, messageSource, messageId, entity)
            }
            stickerDao.saveSticker(chatId, sticker, false)
        }
    }

    private suspend fun preparePngFile(
            bot: DefaultAbsSender,
            sticker: StickerMeta,
    ): File {
        val file = withContext(Dispatchers.IO) { Files.createTempFile(TEMP_FILE_PREFIX, ".webp").toFile() }
        return withTempFile(file) { webpFile ->
            val stickerFile = bot.executeAsync(GetFile().setFileId(sticker.fileId))
            bot.downloadFile(stickerFile, webpFile)
            pngService.webpToPng(webpFile)
        }
    }

    override fun handleInternal(
            bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper,
            userEntity: UserEntity,
    ): Mono<BaseHandler> = mdcMono {
        stateMachine = stateMachine.with { initialState(state.data.state) }
        stateMachine.transition(Event.ReceivedMessage(bot, update, messageSource, userEntity))
    }.thenReturn(this)

    sealed class State {
        object New : State()
        object WaitingForEmoji : State()
        object AllDone : State()
        object Finished : State()
    }

    sealed class Event(
            val bot: DefaultAbsSender,
            val update: Update,
            val messageSource: MessageSourceWrapper,
            val userEntity: UserEntity,
    ) {
        class ReceivedMessage(bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper, userEntity: UserEntity) : Event(bot, update, messageSource, userEntity)
        fun toEventDC(): EventDC = EventDC(bot, update, messageSource, userEntity)
    }

    data class EventDC(val bot: DefaultAbsSender, val update: Update, val messageSource: MessageSourceWrapper, val userEntity: UserEntity)

    sealed class SideEffect {
        class SaveMinimumMeta(val fileId: String, val fileUniqueId: String) : SideEffect()
        class SaveEmoji(val emojiStr: String) : SideEffect()
        class SaveFullMeta(val fileId: String, val fileUniqueId: String, val emojiStr: String) : SideEffect()
    }

    data class NormalStickerHandlerStateData(val state: State, val meta: StickerMeta? = null)

    data class NormalStickerHandlerState(
            override val data: NormalStickerHandlerStateData,
            override val handlerClass: String = NormalStickerHandler2::class.java.name,
    ) : HandlerState<NormalStickerHandlerStateData> {
        override val finished: Boolean
            get() = data.state == State.Finished

    }

    companion object : Loggable

    override fun getState(): HandlerState<NormalStickerHandlerStateData> = state.copy(data = state.data.copy(state = stateMachine.state))

    override fun setState(state: HandlerState<NormalStickerHandlerStateData>) {
        this.state = state as NormalStickerHandlerState
    }

}

