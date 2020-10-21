package com.vdsirotkin.telegram.mystickersbot.handler.common

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.handler.common.DeleteHandler.State.NEW
import com.vdsirotkin.telegram.mystickersbot.handler.common.DeleteHandler.State.PROCESSING
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.executeStickerPackAction
import com.vdsirotkin.telegram.mystickersbot.util.packLink
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.stickers.DeleteStickerFromSet
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import reactor.core.publisher.Mono

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DeleteHandler(
        override val stickerDao: StickerDAO,
        override val messageSource: MessageSource) : LocalizedHandler, StatefulHandler<DeleteHandler.State> {

    private var state: DeleteHandlerState = DeleteHandlerState(NEW, false)

    override fun handleInternal(
            bot: DefaultAbsSender, update: Update,
            messageSource: MessageSourceWrapper,
            userEntity: UserEntity
    ): Mono<BaseHandler> = statefulMdcMono {
        val chatId = update.message.chatId
        when (state.data) {
            NEW -> {
                bot.executeAsync(SendMessage(chatId, messageSource.getMessage("delete.start")).addKeyboard(userEntity, messageSource))
                state = state.copy(data = PROCESSING)
            }
            PROCESSING -> {
                if (update.message.hasSticker()) {
                    val sticker = update.message.sticker
                    val animated = sticker.animated
                    val packName = if (animated) userEntity.animatedPackName else userEntity.normalPackName
                    if (sticker.setName == packName) {
                        bot.executeStickerPackAction(DeleteStickerFromSet(sticker.fileId))
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("delete.success")).setReplyToMessageId(update.message.messageId))
                        state = state.copy(finished = true)
                    } else {
                        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("delete.not.my.sticker")).addKeyboard(userEntity, messageSource))
                    }
                } else {
                    bot.executeAsync(SendMessage(chatId, messageSource.getMessage("delete.please.send.sticker")))
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
        return setReplyMarkup(
                InlineKeyboardMarkup(listOf(listOf(
                        InlineKeyboardButton(messageSource.getMessage("sticker.pack.button.text")).setUrl(packLink(entity.normalPackName)),
                        InlineKeyboardButton(messageSource.getMessage("animated.sticker.pack.button.text")).setUrl(packLink(entity.animatedPackName))
                )))
        )
    }

}
