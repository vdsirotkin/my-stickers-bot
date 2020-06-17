package com.vdsirotkin.telegram.mystickersbot.handler

import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono


@FunctionalInterface
interface BaseHandler {

    fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit>

}
