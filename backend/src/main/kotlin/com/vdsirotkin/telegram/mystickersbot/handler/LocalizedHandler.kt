package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.locale
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.loggerFor

interface LocalizedHandler : BaseHandler {

    val stickerDao: StickerDAO
    val messageSource: MessageSource

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> = mdcMono {
        loggerFor(LocalizedHandler::class.java).info("Executing ${this@LocalizedHandler.javaClass.name}")
        stickerDao.getUserEntity(update.message.chatId).locale
    }.flatMap { handleInternal(bot, update, MessageSourceWrapper(messageSource, it)) }

    fun handleInternal(bot: DefaultAbsSender, update: Update, messageSource: MessageSourceWrapper): Mono<Unit>
}
