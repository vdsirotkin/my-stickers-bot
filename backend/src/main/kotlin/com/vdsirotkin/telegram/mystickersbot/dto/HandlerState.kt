package com.vdsirotkin.telegram.mystickersbot.dto

interface HandlerState<DATA> {
    val finished: Boolean
    val handlerClass: String
    val data: DATA
}
