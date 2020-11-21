package com.vdsirotkin.telegram.mystickersbot.handler.common

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.SendMessage
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

@Service
class UnknownMessageHandler(override val stickerDao: StickerDAO,
                            override val messageSource: MessageSource) : LocalizedHandler {

    override fun handleInternal(
            bot: TelegramBot, update: Update,
            messageSource: MessageSourceWrapper,
            userEntity: UserEntity
    ): Mono<BaseHandler> = mdcMono {
        when {
            update.message() != null -> {
                logger.info("Received some spam: ${update.message().text()}")
                bot.executeAsync(SendMessage(update.message().chat().id(), messageSource["unknown.text"]))
            }
            update.callbackQuery()!=null -> {
                logger.info("Received dangling inline query")
                bot.executeAsync(AnswerCallbackQuery(update.callbackQuery().id())
                        .text(messageSource["unknown.inline.query"]))
            }
            else -> {}
        }
    }.thenReturn(this)

    companion object : Loggable
}
