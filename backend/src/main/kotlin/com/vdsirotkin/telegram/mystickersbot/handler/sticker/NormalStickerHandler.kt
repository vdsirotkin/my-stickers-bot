package com.vdsirotkin.telegram.mystickersbot.handler.sticker

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
class NormalStickerHandler(
        private val pngService: PngService,
        private val stickerPackManagementService: StickerPackManagementService,
        private val stickerPackMessagesSender: StickerPackMessagesSender,
        override val stickerDao: StickerDAO,
        override val messageSource: MessageSource,
) : LocalizedHandler, StatefulHandler<NormalStickerHandler.State> {

    private var state = NormalStickerHandlerState(State.New)

    var stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.New)
        state<State.New> {
            on<Event.ReceivedMessage> {
                val (bot, update, messageSource, entity) = it.toEventDC()
                val chatId = update.message!!.chatId
                val sticker = update.message!!.sticker!!
                if (stickerDao.stickerExists(entity, sticker, false)) {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.already.added")).setReplyToMessageId(update.message!!.messageId))
                    return@on transitionTo(State.Finished)
                }
                logDebug(sticker.toString())
                if (sticker.emoji == null) {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("emoji.required")))
                    transitionTo(State.WaitingForEmoji(StickerMeta(sticker.fileId, sticker.fileUniqueId)))
                } else {
                    transitionTo(State.AllDone(StickerMeta(sticker.fileId, sticker.fileUniqueId, sticker.emoji)))
                }
            }
        }
        state<State.WaitingForEmoji> {
            on<Event.ReceivedMessage> {
                val (bot, update, messageSource, entity) = it.toEventDC()
                val chatId = update.message!!.chatId
                if (update.message.hasText()) {
                    val emojis = EmojiParser.extractEmojis(update.message.text)
                    if (emojis.isEmpty()) {
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                        return@on dontTransition()
                    }
                    val emojiStr = emojis.joinToString()
                    transitionTo(State.AllDone(this@on.meta.copy(emoji = emojiStr)))
                } else {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                    dontTransition()
                }
            }
        }
        state<State.AllDone> {
            onEnter {
                val (bot, update, messageSource, entity) = it.toEventDC()
                val chatId = update.message!!.chatId
                val messageId = update.message!!.messageId
                try {
                    processSticker(bot, this@onEnter.meta, entity, chatId, messageSource, messageId)
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
        state<State.Finished> {  }
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
    ): Mono<BaseHandler> = statefulMdcMono {
        stateMachine = stateMachine.with { initialState(state.data) }
        stateMachine.transition(Event.ReceivedMessage(bot, update, messageSource, userEntity))
        state = state.copy(data = stateMachine.state)
    }

    sealed class State {
        object New : State()
        class WaitingForEmoji(val meta: StickerMeta) : State()
        class AllDone(val meta: StickerMeta) : State()
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

    sealed class SideEffect

    data class NormalStickerHandlerState(
            override val data: State,
            override val handlerClass: String = NormalStickerHandler::class.java.name,
    ) : HandlerState<State> {
        override val finished: Boolean
            get() = data == State.Finished

    }

    companion object : Loggable

    override fun getState(): HandlerState<State> = state

    override fun setState(state: HandlerState<State>) {
        this.state = state as NormalStickerHandlerState
    }

}

