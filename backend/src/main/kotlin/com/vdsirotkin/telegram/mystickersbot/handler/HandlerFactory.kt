package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.handler.common.StartHandler
import com.vdsirotkin.telegram.mystickersbot.handler.common.UnknownMessageHandler
import com.vdsirotkin.telegram.mystickersbot.handler.language.LanguageHandler
import com.vdsirotkin.telegram.mystickersbot.handler.language.SetLanguageHandler
import com.vdsirotkin.telegram.mystickersbot.handler.sticker.AnimatedStickerHandler
import com.vdsirotkin.telegram.mystickersbot.handler.sticker.NormalStickerHandler
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

    @Lookup
    abstract fun languageHandler(): LanguageHandler

    @Lookup
    abstract fun setLanguageHandler(): SetLanguageHandler

}
