package com.vdsirotkin.telegram.mystickersbot.util

import com.vdsirotkin.telegram.mystickersbot.handler.language.LanguageHandler

val availableLocales = listOf(
        LanguageHandler.LanguageInfo("English", "en"),
        LanguageHandler.LanguageInfo("Русский", "ru")
)

const val setLanguageCommandPrefix = "set_language_"

const val TEMP_FILE_PREFIX = "com.vdsirotkin.telegram.mystickersbot-"

const val PNG_SUFFIX = ".png"
const val TGS_SUFFIX = ".tgs"
