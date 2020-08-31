package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState

interface StatefulHandler<DATA> : BaseHandler {

    fun getState(): HandlerState<DATA>

    fun setState(state: HandlerState<DATA>)

}
