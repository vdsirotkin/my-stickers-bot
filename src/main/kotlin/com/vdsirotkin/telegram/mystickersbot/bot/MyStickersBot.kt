package com.vdsirotkin.telegram.mystickersbot.bot

import com.vdsirotkin.telegram.mystickersbot.handler.HandlerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class MyStickersBot(
        private val props: BotConfigProps,
        private val handlerFactory: HandlerFactory
) : TelegramLongPollingBot() {
    override fun onUpdateReceived(update: Update) {
        if (update.message.text == "/start") {
            handlerFactory.startHandler().handle(this, update).subscribe()
        }
    }

    override fun getBotUsername(): String = props.username

    override fun getBotToken(): String = props.token
}
