package com.vdsirotkin.telegram.mystickersbot.util

import org.springframework.context.MessageSource
import java.util.*

class MessageSourceWrapper(
        private val messageSource: MessageSource,
        private val locale: Locale
) {

    fun getMessage(code: String): String = messageSource.getMessage(code, null, locale)

}
