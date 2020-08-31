package com.vdsirotkin.telegram.mystickersbot.dto

interface HandlerState<DATA> {
    val finished: Boolean
    val kclass: String
    val data: DATA
}
