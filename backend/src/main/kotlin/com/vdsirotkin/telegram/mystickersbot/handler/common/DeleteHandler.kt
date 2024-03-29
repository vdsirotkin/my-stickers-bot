package com.vdsirotkin.telegram.mystickersbot.handler.common

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.DeleteStickerFromSet
import com.pengrad.telegrambot.request.SendMessage
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.getPackName
import com.vdsirotkin.telegram.mystickersbot.dto.*
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.handler.common.DeleteHandler.State.NEW
import com.vdsirotkin.telegram.mystickersbot.handler.common.DeleteHandler.State.PROCESSING
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.determineChatId
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.packLink
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DeleteHandler(
    override val stickerDao: StickerDAO,
    override val messageSource: MessageSource
) : LocalizedHandler, StatefulHandler<DeleteHandler.State> {

    private var state: DeleteHandlerState = DeleteHandlerState(NEW, false)

    override fun handleInternal(
        bot: TelegramBot, update: Update,
        messageSource: MessageSourceWrapper,
        userEntity: UserEntity
    ): Mono<BaseHandler> = statefulMdcMono {
        val chatId = determineChatId(update)
        when (state.data) {
            NEW -> {
                bot.executeAsync(SendMessageWithAction(chatId, messageSource["delete.start"], action).addKeyboard(userEntity, messageSource))
                state = state.copy(data = PROCESSING)
            }
            PROCESSING -> {
                if (update.message() != null && update.message().sticker() != null) {
                    val sticker = update.message().sticker()
                    val packName = userEntity.getPackName(sticker.packType())
                    if (sticker.setName() == packName) {
                        bot.executeAsync(DeleteStickerFromSet(sticker.fileId()))
                        stickerDao.deleteSticker(chatId, sticker.fileUniqueId(), sticker.packType())
                        bot.executeAsync(SendMessageWithAction(chatId, messageSource["delete.success"], action).replyToMessageId(update.message().messageId()))
                        state = state.copy(finished = true)
                    } else {
                        bot.executeAsync(SendMessageWithAction(chatId, messageSource["delete.not.my.sticker"], action).addKeyboard(userEntity, messageSource))
                    }
                } else {
                    bot.executeAsync(SendMessageWithAction(chatId, messageSource["delete.please.send.sticker"], action))
                }
            }
        }
    }

    data class DeleteHandlerState(override val data: State, override val finished: Boolean, override val handlerClass: String = DeleteHandler::class.java.name) : HandlerState<State>

    override fun getState(): HandlerState<State> {
        return state
    }

    override fun setState(state: HandlerState<State>) {
        this.state = state as DeleteHandlerState
    }

    enum class State {
        NEW, PROCESSING
    }

    private fun SendMessage.addKeyboard(entity: UserEntity, messageSource: MessageSourceWrapper): SendMessage {
        return replyMarkup(
            InlineKeyboardMarkup(arrayOf(
                InlineKeyboardButton(messageSource["sticker.pack.button.text"]).url(packLink(entity.normalPackName)),
                InlineKeyboardButton(messageSource["animated.sticker.pack.button.text"]).url(packLink(entity.animatedPackName))
            ))
        )
    }

    override val action: String
        get() = "DELETE_STICKER"

}
