package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.*
import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
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
        private val fileHelper: FileHelper,
        override val stickerDao: StickerDAO,
        override val messageSource: MessageSource,
) : LocalizedHandler, StatefulHandler<NormalStickerHandler.State> {

    private var state = NormalStickerHandlerState(State.New)

    var stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.New)
        state<State.New> {
            on<Event.ReceivedMessage> {
                val (bot, update, messageSource, entity) = it.toEventDC()
                val chatId = update.message().chat().id()
                val sticker = update.message().sticker()
                if (stickerDao.stickerExists(entity, sticker)) {
                    bot.executeAsync(SendMessageWithAction(chatId, messageSource["sticker.already.added"], action).replyToMessageId(update.message().messageId()))
                    return@on transitionTo(State.Finished)
                }
                logDebug(sticker.toString())
                if (sticker.emoji() == null) {
                    bot.executeAsync(SendMessageWithAction(chatId, messageSource["emoji.required"], action))
                    transitionTo(State.WaitingForEmoji(StickerMeta(sticker.fileId(), sticker.fileUniqueId(), emoji = null, sticker.packType())))
                } else {
                    transitionTo(State.AllDone(StickerMeta(sticker.fileId(), sticker.fileUniqueId(), sticker.emoji(), sticker.packType())))
                }
            }
        }
        state<State.WaitingForEmoji> {
            on<Event.ReceivedMessage> {
                val (bot, update, messageSource, entity) = it.toEventDC()
                val chatId = update.message().chat().id()
                if (update.message().text() != null) {
                    val emojis = EmojiParser.extractEmojis(update.message().text())
                    if (emojis.isEmpty()) {
                        bot.executeAsync(SendMessageWithAction(chatId, messageSource["send.emojis.message"], action))
                        return@on dontTransition()
                    }
                    val emojiStr = emojis.joinToString()
                    transitionTo(State.AllDone(this@on.meta.copy(emoji = emojiStr)))
                } else {
                    bot.executeAsync(SendMessageWithAction(chatId, messageSource["send.emojis.message"], action))
                    dontTransition()
                }
            }
        }
        state<State.AllDone> {
            internal<Event> {
                val (bot, update, messageSource, entity) = it.toEventDC()
                val chatId = update.message().chat().id()
                val messageId = update.message().messageId()
                try {
                    processSticker(bot, this@internal.meta, entity, chatId, messageSource, messageId)
                    transitionTo(State.Finished)
                } catch (e: PngNotCreatedException) {
                    logger.warn("Can't create png from this sticker")
                    bot.executeAsync(SendMessageWithAction(chatId, messageSource["sticker.cant.be.processed"], action).replyToMessageId(messageId))
                    transitionTo(State.Finished)
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        state<State.Finished> {  }
    }

    private suspend fun processSticker(
            bot: TelegramBot,
            sticker: StickerMeta,
            entity: UserEntity,
            chatId: Long,
            messageSource: MessageSourceWrapper,
            messageId: Int,
    ) {
        withTempFile(preparePngFile(bot, sticker)) {
            if (entity.normalPackCreated) {
                stickerPackManagementService.static().addStickerToPack(bot, chatId, it, entity, sticker.emoji)
                stickerPackMessagesSender.static().sendSuccessAdd(bot, chatId, messageSource, messageId, entity, action)
            } else {
                stickerPackManagementService.static().createNewPack(bot, chatId, it, entity, sticker.emoji)
                stickerDao.createSet(chatId, StickerPackType.NORMAL, entity.normalPackName)
                stickerPackMessagesSender.static().sendSuccessCreated(bot, chatId, messageSource, messageId, entity, action)
            }
            stickerDao.saveSticker(chatId, sticker)
        }
    }

    private suspend fun preparePngFile(
            bot: TelegramBot,
            sticker: StickerMeta,
    ): File {
        val file = withContext(Dispatchers.IO) { Files.createTempFile(TEMP_FILE_PREFIX, ".webp").toFile() }
        return withTempFile(file) { webpFile ->
            val stickerFile = bot.executeAsync(GetFile(sticker.fileId))
            if (!stickerFile.isOk) {
                throw PngNotCreatedException()
            }
            fileHelper.downloadFile(bot, stickerFile.file().fileId(), webpFile)
            pngService.webpToPng(webpFile)
        }
    }

    override fun handleInternal(
            bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper,
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
            val bot: TelegramBot,
            val update: Update,
            val messageSource: MessageSourceWrapper,
            val userEntity: UserEntity,
    ) {
        class ReceivedMessage(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper, userEntity: UserEntity) : Event(bot, update, messageSource, userEntity)
        fun toEventDC(): EventDC = EventDC(bot, update, messageSource, userEntity)
    }

    data class EventDC(val bot: TelegramBot, val update: Update, val messageSource: MessageSourceWrapper, val userEntity: UserEntity)

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

    override val action: String
        get() = "NORMAL_STICKER"

}

