package com.vdsirotkin.telegram.mystickersbot.handler.language

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import com.vdsirotkin.telegram.mystickersbot.util.setLanguageCommandPrefix
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import java.util.*

@Component
class SetLanguageHandler(
        private val messageSource: MessageSource,
        private val stickerDAO: StickerDAO
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> = mdcMono {
        val chatId = update.callbackQuery.from.id.toLong()
        val selectedLanguage = update.callbackQuery.data.removePrefix(setLanguageCommandPrefix)
        stickerDAO.saveUserLanguage(chatId, selectedLanguage)
        bot.executeAsync(AnswerCallbackQuery()
                .setCallbackQueryId(update.callbackQuery.id)
                .setText(messageSource.getMessage("success.chosen", null, Locale.forLanguageTag(selectedLanguage)))
        )
    }.thenReturn(Unit)

}
