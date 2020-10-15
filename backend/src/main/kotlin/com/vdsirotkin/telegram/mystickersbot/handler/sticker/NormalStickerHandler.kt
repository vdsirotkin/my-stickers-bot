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
        override val messageSource: MessageSource
) : LocalizedHandler, StatefulHandler<NormalStickerHandler.HandlerStateData> {

    private var state: NormalStickerHandlerState = NormalStickerHandlerState(HandlerStateData(State.NEW), false)

    override fun handleInternal(bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper): Mono<BaseHandler> = mdcMono {
        val chatId = update.message!!.chatId
        runStateMachine(chatId, update, bot, messageSource)
    }.thenReturn(this)

    private suspend fun runStateMachine(
            chatId: Long,
            update: Update,
            bot: DefaultAbsSender,
            messageSource: MessageSourceWrapper) {
        val entity = stickerDao.getUserEntity(chatId)
        val messageId = update.message!!.messageId
        when (state.data.state) {
            State.NEW -> {
                val sticker = update.message!!.sticker!!
                if (stickerDao.stickerExists(entity, sticker, false)) {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.already.added")).setReplyToMessageId(update.message!!.messageId))
                    return
                }
                logDebug(sticker.toString())
                if (sticker.emoji == null) {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("emoji.required")))
                    state = state.toEmojiRequired(StickerMeta(sticker.fileId, sticker.fileUniqueId))
                } else {
                    state = state.toAllDone(StickerMeta(sticker.fileId, sticker.fileUniqueId, sticker.emoji))
                    runStateMachine(chatId, update, bot, messageSource)
                }
            }
            State.EMOJI_REQUIRED -> {
                if (update.message.hasText()) {
                    val emojis = EmojiParser.extractEmojis(update.message.text)
                    if (emojis.isEmpty()) {
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                        return
                    }
                    val emojiStr = emojis.joinToString()
                    state = state.toAllDone(state.data.stickerMeta!!.copy(emoji = emojiStr))
                    runStateMachine(chatId, update, bot, messageSource)
                } else {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("send.emojis.message")))
                }
            }
            State.ALL_DONE -> {
                try {
                    processSticker(bot, state.data.stickerMeta!!, entity, chatId, messageSource, messageId)
                } catch (e: PngNotCreatedException) {
                    logger.warn("Can't create png from this sticker")
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.cant.be.processed")).setReplyToMessageId(messageId))
                } catch (e: Exception) {
                    throw e
                } finally {
                    state = state.finished()
                }
            }
        }
    }

    private suspend fun processSticker(
            bot: DefaultAbsSender,
            sticker: StickerMeta,
            entity: UserEntity,
            chatId: Long,
            messageSource: MessageSourceWrapper,
            messageId: Int) {
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

    private suspend fun preparePngFile(bot: DefaultAbsSender,
                                       sticker: StickerMeta): File {
        val file = withContext(Dispatchers.IO) { Files.createTempFile(TEMP_FILE_PREFIX, ".webp").toFile() }
        return withTempFile(file) { webpFile ->
            val stickerFile = bot.executeAsync(GetFile().setFileId(sticker.fileId))
            bot.downloadFile(stickerFile, webpFile)
            pngService.webpToPng(webpFile)
        }
    }

    override fun getState(): HandlerState<HandlerStateData> = state

    override fun setState(state: HandlerState<HandlerStateData>) {
        this.state = state as NormalStickerHandlerState
    }

    data class NormalStickerHandlerState(override val data: HandlerStateData,
                                         override val finished: Boolean,
                                         override val handlerClass: String = NormalStickerHandler::class.java.name) : HandlerState<HandlerStateData>

    private fun NormalStickerHandlerState.toEmojiRequired(stickerMeta: StickerMeta): NormalStickerHandlerState {
        return this.copy(data = this.data.copy(state = State.EMOJI_REQUIRED, stickerMeta = stickerMeta))
    }

    private fun NormalStickerHandlerState.toAllDone(meta: StickerMeta): NormalStickerHandlerState {
        return this.copy(data = this.data.copy(state = State.ALL_DONE, stickerMeta = meta))
    }

    private fun NormalStickerHandlerState.finished(): NormalStickerHandlerState {
        return this.copy(finished = true)
    }

    data class HandlerStateData(val state: State, val stickerMeta: StickerMeta? = null)

    enum class State {
        NEW, EMOJI_REQUIRED, ALL_DONE
    }

    companion object : Loggable
}

