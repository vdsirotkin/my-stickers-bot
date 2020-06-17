package com.vdsirotkin.telegram.mystickersbot.bot

import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.HandlerFactory
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable
import java.util.*

@Service
class MyStickersBot(
        private val props: BotConfigProps,
        private val handlerFactory: HandlerFactory
) : TelegramLongPollingBot() {
    override fun onUpdateReceived(update: Update) {
        Mono.just("")
                .map {
                    when {
                        update.message.text == "/start" -> handlerFactory.startHandler()
                        update.message.hasSticker() -> processSticker(update.message.sticker)
                        else -> handlerFactory.unknownMessageHandler()
                    }
                }
                .flatMap { it.handle(this, update) }
                .doOnEach {
                    if (it.isOnError) {
                        val callId: String = it.context["callId"]
                        logger.error("Error occurred, callId=$callId, message: ${it.throwable!!.message}", it.throwable)
                        sendErrorMessagesAsync(update.message.chatId, callId)
                    }
                }.subscriberContext { it.put("callId", UUID.randomUUID().toString()) }.subscribe()
    }

    private fun processSticker(sticker: Sticker): BaseHandler {
        return if (sticker.animated) {
            handlerFactory.animatedStickerHandler()
        } else {
            handlerFactory.normalStickerHandler()
        }
    }

    private fun sendErrorMessagesAsync(chatId: Long, callId: String) {
        GlobalScope.launch {
            try {
                executeAsync(SendMessage(props.serviceAccountId, "Error occurred, check call id $callId"))
                executeAsync(SendMessage(chatId, "Sorry, some error occurred :( Admin is already notified, the problem will be fixed soon"))
            } catch (e: Exception) {
                logger.error("Unrecoverable error, callId=$callId, message: ${e.message}", e)
            }
        }
    }

    override fun getBotUsername(): String = props.username

    override fun getBotToken(): String = props.token

    companion object : Loggable
}
