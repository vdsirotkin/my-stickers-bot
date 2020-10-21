package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.exception.HandlerException
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import kotlinx.coroutines.CoroutineScope
import reactor.core.publisher.Mono

interface StatefulHandler<DATA> : BaseHandler {

    fun getState(): HandlerState<DATA>

    fun setState(state: HandlerState<DATA>)

    fun statefulMdcMono(block: suspend CoroutineScope.() -> Unit): Mono<BaseHandler> {
        return mdcMono {
            block()
        }.onErrorMap { HandlerException(it, this) }.thenReturn(this)
    }

}
