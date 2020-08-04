package com.vdsirotkin.telegram.mystickersbot.handler.photo

import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono

@Component
class DocumentHandler(override val stickerDao: StickerDAO, override val messageSource: MessageSource) : LocalizedHandler {


    override fun handleInternal(bot: DefaultAbsSender, update: Update,
                                messageSource: MessageSourceWrapper): Mono<Unit> = mdcMono {
        bot.executeAsync(SendMessage(update.message.chatId, messageSource.getMessage("document.not.supported")))
        return@mdcMono
    }


}
