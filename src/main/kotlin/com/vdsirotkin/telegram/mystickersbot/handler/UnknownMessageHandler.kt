package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono

@Service
class UnknownMessageHandler : BaseHandler {
    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> = mono(Dispatchers.IO) {
        bot.executeAsync(SendMessage(update.message.chatId, "Unknown command, please send me just stickers :)"))
    }.thenReturn(Unit)
}
