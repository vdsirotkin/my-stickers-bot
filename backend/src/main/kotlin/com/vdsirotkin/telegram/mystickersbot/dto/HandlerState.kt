package com.vdsirotkin.telegram.mystickersbot.dto

interface HandlerState<T> {
    val finished: Boolean
    val kclass: String
    val data: T
}
