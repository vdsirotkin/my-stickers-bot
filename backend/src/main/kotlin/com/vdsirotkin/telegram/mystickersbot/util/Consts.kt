package com.vdsirotkin.telegram.mystickersbot.util

import com.vdsirotkin.telegram.mystickersbot.handler.language.LanguageHandler

val availableLocales = listOf(
        LanguageHandler.LanguageInfo("English", "en"),
        LanguageHandler.LanguageInfo("Русский", "ru")
)

const val setLanguageCommandPrefix = "set_language_"
const val chooseEmoji = "choose_emoji_"

const val TEMP_FILE_PREFIX = "com.vdsirotkin.telegram.mystickersbot-"

const val PNG_SUFFIX = ".png"
const val TGS_SUFFIX = ".tgs"

const val MDC_USER_ID = "userId"
const val MDC_CALL_ID = "callId"

const val GROUP_MESSAGE = "Sorry, i don't support groups. Please remove me from this chat."