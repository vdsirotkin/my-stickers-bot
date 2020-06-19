package com.vdsirotkin.telegram.mystickersbot.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.updateshandlers.DownloadFileCallback
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.io.File
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

const val MDC_CALL_ID = "callId"
const val MDC_USER_ID = "userId"

fun SendMessage.addInlineKeyboard(title: String, url: String): SendMessage {
    this.replyMarkup = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton(title).setUrl(url))))
    return this
}

suspend fun <T> withTempFile(file: File, context: CoroutineContext = Dispatchers.IO, block: suspend (File) -> T): T {
     return withContext(context) {
        val result = block(file)
        file.delete()
        result
    }
}

suspend fun <T : Serializable> DefaultAbsSender.executeAsync(method: BotApiMethod<T>): T {
    return suspendCancellableCoroutine { cont ->
        this.executeAsync(method, object : SentCallback<T> {
            override fun onResult(method: BotApiMethod<T>?, p1: T) {
                cont.resume(p1)
            }

            override fun onException(method: BotApiMethod<T>?, p1: Exception?) {
                cont.cancel(p1)
            }

            override fun onError(method: BotApiMethod<T>?, p1: TelegramApiRequestException?) {
                cont.cancel(p1)
            }
        })
    }
}

suspend fun DefaultAbsSender.downloadFileAsync(filePath: String): File {
    return suspendCancellableCoroutine { cont ->
        this.downloadFileAsync(filePath, object : DownloadFileCallback<String?> {
            override fun onResult(p0: String?, p1: File?) {
                cont.resume(p1!!)
            }

            override fun onException(p0: String?, p1: java.lang.Exception?) {
                cont.cancel(p1)
            }
        })
    }
}

fun <T> monoWithMdc(context: CoroutineContext = EmptyCoroutineContext,
                block: suspend CoroutineScope.() -> T?): Mono<T> {
    return mono(context) {
        coroutineContext.resolveMdc()
        val result = block()
        MDC.clear()
        result
    }
}

fun Context.resolveMdc() {
    MDC.put(MDC_CALL_ID, this[MDC_CALL_ID])
    MDC.put(MDC_USER_ID, this[MDC_USER_ID])
}

@Suppress("EXPERIMENTAL_API_USAGE")
fun CoroutineContext.resolveMdc() {
    this[ReactorContext]?.context?.resolveMdc()
}
