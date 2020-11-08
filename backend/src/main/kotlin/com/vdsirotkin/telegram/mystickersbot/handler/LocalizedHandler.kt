package com.vdsirotkin.telegram.mystickersbot.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.locale
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.determineChatId
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.loggerFor

interface LocalizedHandler : BaseHandler {

    val stickerDao: StickerDAO
    val messageSource: MessageSource

    override fun handle(bot: TelegramBot, update: Update): Mono<BaseHandler> = mdcMono {
        loggerFor(LocalizedHandler::class.java).info("Executing ${this@LocalizedHandler.javaClass.name}")
        stickerDao.getUserEntity(determineChatId(update))
    }.flatMap { handleInternal(bot, update, MessageSourceWrapper(messageSource, it.locale), it) }

    fun handleInternal(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper,
                       userEntity: UserEntity): Mono<BaseHandler>
}
