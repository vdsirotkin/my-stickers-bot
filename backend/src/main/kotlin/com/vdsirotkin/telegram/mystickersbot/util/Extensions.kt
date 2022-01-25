package com.vdsirotkin.telegram.mystickersbot.util

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import reactor.core.publisher.Mono
import reactor.util.context.Context
import ru.sokomishalov.commons.core.log.loggerFor
import ru.sokomishalov.commons.core.serialization.OBJECT_MAPPER
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

val DELETE_TEMP = System.getProperty("delete.temp", "true")!!.toBoolean()
val LOG_RESPONSES = System.getProperty("log.responses", "false")!!.toBoolean()

fun SendMessage.addInlineKeyboard(title: String, url: String): SendMessage {
    replyMarkup(InlineKeyboardMarkup(arrayOf(InlineKeyboardButton(title).url(url))))
    return this
}

suspend fun execWithLog(command: String) {
    withContext(Dispatchers.IO) {
        loggerFor(Runtime::class.java).info("Executing: $command")
        Runtime.getRuntime().exec(command).inputStream.let {
            BufferedReader(InputStreamReader(it)).readLines()
        }
    }
}

suspend fun <T> withTempFile(file: File, context: CoroutineContext = Dispatchers.IO,
                             block: suspend (File) -> T): T {
    return withContext(context) {
        val result = try {
            block(file)
        } catch (e: Exception) {
            throw e
        } finally {
            if (DELETE_TEMP) {
                file.delete()
            } else {
                loggerFor("withTempFile").info(file.absolutePath)
            }
        }
        result
    }
}

suspend fun <T> withTempFiles(file: Array<File>, context: CoroutineContext = Dispatchers.IO,
                              block: suspend () -> T): T {
    return withContext(context) {
        val result = try {
            block()
        } catch (e: Exception) {
            throw e
        } finally {
            if (DELETE_TEMP) {
                file.forEach { it.delete() }
            } else {
                val logger = loggerFor("withTempFile")
                file.forEach { logger.info(it.absolutePath) }
            }
        }
        result
    }
}

suspend fun <T : BaseRequest<T, R>, R : BaseResponse> TelegramBot.executeAsync(method: T): R {
    return wrapApiCall {
        suspendCancellableCoroutine { cont ->
            execute(method, object : Callback<T, R> {
                override fun onResponse(request: T?, response: R) {
                    if (LOG_RESPONSES) {
                        loggerFor("responseLogger").info(OBJECT_MAPPER.writeValueAsString(response))
                    }
                    cont.resume(response)
                }

                override fun onFailure(request: T?, e: IOException?) {
                    cont.cancel(e)
                }
            })
        }
    }
}

suspend fun <T> TelegramBot.wrapApiCall(block: suspend TelegramBot.() -> T): T {
    return if (this is MyStickersBot) {
        retry.executeSuspendFunction {
            rateLimiter.executeSuspendFunction {
                block()
            }
        }
    } else {
        block()
    }
}

fun <T> mdcMono(context: CoroutineContext = EmptyCoroutineContext,
                block: suspend CoroutineScope.() -> T?): Mono<T> {
    return mono(context) {
        coroutineContext.resolveMdc()
        withContext(MDCContext()) {
            block()
        }
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

fun determineChatId(update: Update): Long {
    return when {
        update.message() != null -> update.message().chat().id()
        update.callbackQuery() != null -> update.callbackQuery().from().id()
        else -> throw IllegalArgumentException("Unsupported message type")
    }
}
