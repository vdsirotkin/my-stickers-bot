package com.vdsirotkin.telegram.mystickersbot.handler

import com.pengrad.telegrambot.TelegramBot
import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.exception.HandlerException
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import kotlinx.coroutines.CoroutineScope
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.loggerFor

interface StatefulHandler<DATA> : BaseHandler {

    fun getState(): HandlerState<DATA>

    fun setState(state: HandlerState<DATA>)

    suspend fun cancel(bot: TelegramBot, chatId: Long) {
        loggerFor(StatefulHandler::class).info("Nothing to cancel")
    }

    fun statefulMdcMono(block: suspend CoroutineScope.() -> Unit): Mono<BaseHandler> {
        return mdcMono {
            block()
        }.onErrorMap { HandlerException(it, this) }.thenReturn(this)
    }

}
