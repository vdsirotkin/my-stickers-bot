package com.vdsirotkin.telegram.mystickersbot.service

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.locale
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service

@Service
class LocalizedMessageSourceProvider(
        private val stickerDAO: StickerDAO,
        private val messageSource: MessageSource
) {

    suspend fun getMessageSource(chatId: Long): MessageSourceWrapper {
        val locale = stickerDAO.getUserEntity(chatId).locale
        return MessageSourceWrapper(messageSource, locale)
    }

}
