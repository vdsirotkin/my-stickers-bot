package com.vdsirotkin.telegram.mystickersbot.handler.sticker

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.request.SendDocument
import com.pengrad.telegrambot.request.SendPhoto
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import com.vdsirotkin.telegram.mystickersbot.dto.packType
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.handler.sticker.DownloadStickerHandler.State
import com.vdsirotkin.telegram.mystickersbot.service.FileHelper
import com.vdsirotkin.telegram.mystickersbot.service.image.PngService
import com.vdsirotkin.telegram.mystickersbot.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Files

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DownloadStickerHandler(
    private val fileHelper: FileHelper,
    private val pngService: PngService,
    override val stickerDao: StickerDAO,
    override val messageSource: MessageSource
) : LocalizedHandler, StatefulHandler<State> {

    private var state = DownloadStickerHandlerState(State.New)

    sealed class State {
        object New : State()
        object WaitingForSticker : State()
        object Finished : State()
    }

    data class ReceivedMessageEvent(
        val bot: TelegramBot,
        val update: Update,
        val messageSource: MessageSourceWrapper
    )

    sealed class SideEffect

    private var stateMachine = StateMachine.create<State, ReceivedMessageEvent, SideEffect> {
        initialState(State.New)

        state<State.New> {
            on<ReceivedMessageEvent> {
                val (bot, update, messageSource) = it
                bot.executeAsync(SendMessageWithAction(update.message().chat().id(), messageSource["send.sticker.download"], action))
                transitionTo(State.WaitingForSticker)
            }
        }
        state<State.WaitingForSticker> {
            on<ReceivedMessageEvent> {
                val (bot, update, messageSource) = it
                val sticker = update.message().sticker()
                if (sticker == null) {
                    bot.executeAsync(SendMessageWithAction(update.message().chat().id(), messageSource["not.sticker.download"], action).replyToMessageId(update.message().messageId()))
                    return@on dontTransition()
                }
                val pngFile = preparePngFile(bot, StickerMeta(sticker.fileId(), sticker.fileUniqueId(), emoji = null, sticker.packType()))
                withTempFile(pngFile) { file ->
                    bot.executeAsync(SendDocument(update.message().chat().id(), file).replyToMessageId(update.message().messageId()))
                    bot.executeAsync(SendPhoto(update.message().chat().id(), file).replyToMessageId(update.message().messageId()))
                }
                transitionTo(State.Finished)
            }
        }
        state<State.Finished> {  }
    }

    private suspend fun preparePngFile(
        bot: TelegramBot,
        sticker: StickerMeta,
    ): File {
        val file = withContext(Dispatchers.IO) { Files.createTempFile(TEMP_FILE_PREFIX, ".webp").toFile() }
        return withTempFile(file) { webpFile ->
            val stickerFile = bot.executeAsync(GetFile(sticker.fileId))
            fileHelper.downloadFile(bot, stickerFile.file().fileId(), webpFile)
            pngService.webpToPng(webpFile)
        }
    }

    override fun handleInternal(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper, userEntity: UserEntity): Mono<BaseHandler> = statefulMdcMono {
        stateMachine = stateMachine.with { initialState(state.data) }
        stateMachine.transition(ReceivedMessageEvent(bot, update, messageSource))
        state = state.copy(data = stateMachine.state)
    }

    override fun getState(): HandlerState<State> = state

    override fun setState(state: HandlerState<State>) {
        this.state = state as DownloadStickerHandlerState
    }

    data class DownloadStickerHandlerState(
        override val data: State,
        override val handlerClass: String = DownloadStickerHandler::class.java.name,
    ) : HandlerState<State> {
        override val finished: Boolean
            get() = data == State.Finished

    }

    override val action: String
        get() = "DOWNLOAD_STICKER"
}
