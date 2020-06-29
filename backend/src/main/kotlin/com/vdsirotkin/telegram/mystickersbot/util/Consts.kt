package com.vdsirotkin.telegram.mystickersbot.util

import com.vdsirotkin.telegram.mystickersbot.handler.language.LanguageHandler

val availableLocales = listOf(
        LanguageHandler.LanguageInfo("English", "en"),
        LanguageHandler.LanguageInfo("Русский", "ru")
)

const val setLanguageCommandPrefix = "set_language_"
