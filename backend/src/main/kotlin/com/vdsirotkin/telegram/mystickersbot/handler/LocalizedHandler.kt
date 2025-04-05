package com.vdsirotkin.telegram.mystickersbot.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.locale
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.determineChatId
import org.springframework.context.MessageSource
import ru.sokomishalov.commons.core.log.loggerFor

interface LocalizedHandler : BaseHandler {

    val stickerDao: StickerDAO
    val messageSource: MessageSource

    override suspend fun handle(bot: TelegramBot, update: Update) {
        loggerFor(LocalizedHandler::class.java).info("Executing ${this@LocalizedHandler.javaClass.name}")
        val userEntity = stickerDao.getUserEntity(determineChatId(update))
        handleInternal(bot, update, MessageSourceWrapper(messageSource, userEntity.locale), userEntity)
    }

    suspend fun handleInternal(bot: TelegramBot, update: Update, messageSource: MessageSourceWrapper,
                      userEntity: UserEntity)
}
