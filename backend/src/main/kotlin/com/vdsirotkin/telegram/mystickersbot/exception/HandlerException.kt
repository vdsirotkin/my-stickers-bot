package com.vdsirotkin.telegram.mystickersbot.exception

import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler

class HandlerException(
        val parent: Throwable,
        val handler: BaseHandler
) : Throwable(parent)
