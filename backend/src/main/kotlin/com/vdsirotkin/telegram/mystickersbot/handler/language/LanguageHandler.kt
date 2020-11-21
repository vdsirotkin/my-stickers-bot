package com.vdsirotkin.telegram.mystickersbot.handler.language

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.util.availableLocales
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import com.vdsirotkin.telegram.mystickersbot.util.setLanguageCommandPrefix
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*

@Component
class LanguageHandler(
        private val messageSource: MessageSource
) : BaseHandler {

    override fun handle(bot: TelegramBot, update: Update): Mono<BaseHandler> {
        return mdcMono {
            val message = messageSource.getMessage("choose.language", emptyArray(), Locale.ENGLISH)
            bot.executeAsync(SendMessage(update.message().chat().id(), message)
                    .replyMarkup(InlineKeyboardMarkup(availableLocales.map {
                        InlineKeyboardButton(it.name).callbackData("$setLanguageCommandPrefix${it.code}")
                    }.toTypedArray())))
        }.thenReturn(this)
    }

    data class LanguageInfo(val name: String, val code: String)

}
