package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.dao.locale
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import kotlinx.coroutines.reactor.mono
import org.springframework.context.MessageSource
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono

interface LocalizedHandler : BaseHandler {

    val stickerDao: StickerDAO
    val messageSource: MessageSource

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> = mono {
        stickerDao.getUserEntity(update.message.chatId).locale
    }.flatMap { handleInternal(bot, update, MessageSourceWrapper(messageSource, it)) }

    fun handleInternal(bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper): Mono<Unit>
}
