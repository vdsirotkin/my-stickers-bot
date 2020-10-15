package com.vdsirotkin.telegram.mystickersbot

import com.vdsirotkin.telegram.mystickersbot.util.MDC_CALL_ID
import com.vdsirotkin.telegram.mystickersbot.util.MDC_USER_ID
import reactor.core.publisher.Mono
import java.util.*

internal fun <T> Mono<T>.fillMdc(chatId: Long = 123123L): Mono<T> {
    return this.subscriberContext {
        it.put(MDC_USER_ID, chatId.toString()).put(MDC_CALL_ID, UUID.randomUUID().toString())
    }
}
