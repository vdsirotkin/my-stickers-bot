package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.monoWithMdc
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

@Service
class UnknownMessageHandler : BaseHandler {
    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> = monoWithMdc {
        logger.info("Received some spam: ${update.message.text}")
        bot.executeAsync(SendMessage(update.message.chatId, "Unknown command, please send me just stickers :)"))
    }.thenReturn(Unit)

    companion object : Loggable
}
