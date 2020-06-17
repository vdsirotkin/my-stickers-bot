package com.vdsirotkin.telegram.mystickersbot.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.updateshandlers.DownloadFileCallback
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import java.io.File
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

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
    return suspendCancellableCoroutine<T> { cont ->
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
