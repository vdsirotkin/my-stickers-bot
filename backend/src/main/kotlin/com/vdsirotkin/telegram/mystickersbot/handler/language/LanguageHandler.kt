package com.vdsirotkin.telegram.mystickersbot.handler.language

import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.util.availableLocales
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import com.vdsirotkin.telegram.mystickersbot.util.setLanguageCommandPrefix
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import reactor.core.publisher.Mono
import java.util.*

@Component
class LanguageHandler(
        private val messageSource: MessageSource
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<BaseHandler> {
        return mdcMono {
            val message = messageSource.getMessage("choose.language", emptyArray(), Locale.ENGLISH)
            bot.executeAsync(SendMessage(update.message!!.chatId, message)
                    .setReplyMarkup(InlineKeyboardMarkup(listOf(availableLocales.map {
                        InlineKeyboardButton(it.name).apply { callbackData = "$setLanguageCommandPrefix${it.code}" }
                    }))))
        }.thenReturn(this)
    }

    data class LanguageInfo(val name: String, val code: String)

}
