package com.vdsirotkin.telegram.mystickersbot.handler

import org.springframework.beans.factory.annotation.Lookup
import org.springframework.stereotype.Service

@Service
abstract class HandlerFactory {

    @Lookup
    abstract fun startHandler(): StartHandler

    @Lookup
    abstract fun normalStickerHandler(): NormalStickerHandler

    @Lookup
    abstract fun animatedStickerHandler(): AnimatedStickerHandler

    @Lookup
    abstract fun unknownMessageHandler(): UnknownMessageHandler

}
