package com.vdsirotkin.telegram.mystickersbot.handler.language

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import com.vdsirotkin.telegram.mystickersbot.util.setLanguageCommandPrefix
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*

@Component
class SetLanguageHandler(
        private val messageSource: MessageSource,
        private val stickerDAO: StickerDAO
) : BaseHandler {

    override fun handle(bot: TelegramBot, update: Update): Mono<BaseHandler> = mdcMono {
        val chatId = update.callbackQuery().from().id().toLong()
        val selectedLanguage = update.callbackQuery().data().removePrefix(setLanguageCommandPrefix)
        stickerDAO.saveUserLanguage(chatId, selectedLanguage)
        bot.executeAsync(AnswerCallbackQuery(update.callbackQuery().id())
                .text(messageSource.getMessage("success.chosen", null, Locale.forLanguageTag(selectedLanguage)))
        )
    }.thenReturn(this)

}
