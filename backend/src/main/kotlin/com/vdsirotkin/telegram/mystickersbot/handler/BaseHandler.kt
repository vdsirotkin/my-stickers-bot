package com.vdsirotkin.telegram.mystickersbot.handler

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import reactor.core.publisher.Mono


interface BaseHandler {

    val action: String

    fun handle(bot: TelegramBot, update: Update): Mono<BaseHandler>

}
