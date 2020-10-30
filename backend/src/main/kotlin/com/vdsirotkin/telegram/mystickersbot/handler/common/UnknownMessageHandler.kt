package com.vdsirotkin.telegram.mystickersbot.handler.common

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

@Service
class UnknownMessageHandler(override val stickerDao: StickerDAO,
                            override val messageSource: MessageSource) : LocalizedHandler {

    override fun handleInternal(
            bot: DefaultAbsSender, update: Update,
            messageSource: MessageSourceWrapper,
            userEntity: UserEntity
    ): Mono<BaseHandler> = mdcMono {
        when {
            update.hasMessage() -> {
                logger.info("Received some spam: ${update.message.text}")
                bot.executeAsync(SendMessage(update.message.chatId, messageSource.getMessage("unknown.text")))
            }
            update.hasCallbackQuery() -> {
                logger.info("Received dangling inline query")
                bot.executeAsync(AnswerCallbackQuery()
                        .setCallbackQueryId(update.callbackQuery.id)
                        .setText(messageSource.getMessage("unknown.inline.query")))
            }
            else -> {}
        }
    }.thenReturn(this)

    companion object : Loggable
}
