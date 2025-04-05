package com.vdsirotkin.telegram.mystickersbot.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update

interface BaseHandler {

    val action: String

    suspend fun handle(bot: TelegramBot, update: Update)

}
