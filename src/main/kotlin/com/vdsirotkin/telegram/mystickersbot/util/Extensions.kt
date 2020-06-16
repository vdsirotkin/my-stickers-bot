package com.vdsirotkin.telegram.mystickersbot.util

import com.github.kotlintelegrambot.network.Response

inline fun <reified T> Pair<retrofit2.Response<Response<T>?>?, Exception?>.body(): T? {
    val (response, exception) = this
    if (exception != null) {
        throw exception
    }
    if (response?.isSuccessful == true) {
        return response.body()?.result
    } else {
        throw NonOkTelegramResponseException(response?.errorBody()?.bytes()?.toString(Charsets.UTF_8))
    }
}

class NoBodyException : Exception("No body in telegram response...")

class NonOkTelegramResponseException(override val message: String?) : Exception(message)
