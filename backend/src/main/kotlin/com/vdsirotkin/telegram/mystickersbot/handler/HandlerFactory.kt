package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.handler.common.DeleteHandler
import com.vdsirotkin.telegram.mystickersbot.handler.common.StartHandler
import com.vdsirotkin.telegram.mystickersbot.handler.common.UnknownMessageHandler
import com.vdsirotkin.telegram.mystickersbot.handler.language.LanguageHandler
import com.vdsirotkin.telegram.mystickersbot.handler.language.SetLanguageHandler
import com.vdsirotkin.telegram.mystickersbot.handler.photo.DocumentHandler
import com.vdsirotkin.telegram.mystickersbot.handler.photo.PhotoHandler
import com.vdsirotkin.telegram.mystickersbot.handler.sticker.AnimatedStickerHandler
import com.vdsirotkin.telegram.mystickersbot.handler.sticker.DownloadStickerHandler
import com.vdsirotkin.telegram.mystickersbot.handler.sticker.NormalStickerHandler
import org.springframework.beans.factory.annotation.Lookup
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
abstract class HandlerFactory(
        private val applicationContext: ApplicationContext
) {

    @get:Lookup
    abstract val startHandler: StartHandler

    @get:Lookup
    abstract val normalStickerHandler: NormalStickerHandler

    @get:Lookup
    abstract val animatedStickerHandler: AnimatedStickerHandler

    @get:Lookup
    abstract val unknownMessageHandler: UnknownMessageHandler

    @get:Lookup
    abstract val languageHandler: LanguageHandler

    @get:Lookup
    abstract val setLanguageHandler: SetLanguageHandler

    @get:Lookup
    abstract val photoHandler: PhotoHandler

    @get:Lookup
    abstract val documentHandler: DocumentHandler

    @get:Lookup
    abstract val deleteHandler: DeleteHandler

    @get:Lookup
    abstract val downloadHandler: DownloadStickerHandler

    fun newHandler(kClass: KClass<out BaseHandler>): BaseHandler = applicationContext.getBean(kClass.java)

}
