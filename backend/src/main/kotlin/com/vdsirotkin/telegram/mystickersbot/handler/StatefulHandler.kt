package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState

interface StatefulHandler<T> : BaseHandler {

    fun getState(): HandlerState<T>

    fun setState(state: HandlerState<T>)

}
