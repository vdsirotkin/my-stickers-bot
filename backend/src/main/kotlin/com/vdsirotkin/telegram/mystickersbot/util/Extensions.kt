package com.vdsirotkin.telegram.mystickersbot.util

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
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.stickers.AddStickerToSet
import org.telegram.telegrambots.meta.api.methods.stickers.CreateNewStickerSet
import org.telegram.telegrambots.meta.api.methods.stickers.DeleteStickerFromSet
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.updateshandlers.DownloadFileCallback
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import reactor.core.publisher.Mono
import reactor.util.context.Context
import ru.sokomishalov.commons.core.log.loggerFor
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

val DELETE_TEMP = System.getProperty("delete.temp", "true")!!.toBoolean()

fun SendMessage.addInlineKeyboard(title: String, url: String): SendMessage {
    this.replyMarkup = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton(title).setUrl(url))))
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

suspend fun <T : Serializable> DefaultAbsSender.executeAsync(method: BotApiMethod<T>): T {
    return wrapApiCall {
        suspendCancellableCoroutine<T> { cont ->
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
}

suspend fun <T> DefaultAbsSender.wrapApiCall(block: suspend DefaultAbsSender.() -> T): T {
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

suspend fun DefaultAbsSender.executeStickerPackAction(method: PartialBotApiMethod<Boolean>) {
    when (method) {
        is CreateNewStickerSet -> wrapApiCall { execute(method) }
        is AddStickerToSet -> wrapApiCall { execute(method) }
        is DeleteStickerFromSet -> wrapApiCall { execute(method) }
        else -> throw UnsupportedOperationException("${method.javaClass} is not supported by executeStickerPack")
    }
}

suspend fun DefaultAbsSender.downloadFileAsync(filePath: String): File {
    return wrapApiCall {
        suspendCancellableCoroutine<File> { cont ->
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
        update.hasMessage() -> update.message.chatId
        update.hasCallbackQuery() -> update.callbackQuery.from.id.toLong()
        else -> throw IllegalArgumentException("Unsupported message type")
    }
}
