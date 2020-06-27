package com.vdsirotkin.telegram.mystickersbot.bot

import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.HandlerFactory
import com.vdsirotkin.telegram.mystickersbot.util.MDC_CALL_ID
import com.vdsirotkin.telegram.mystickersbot.util.MDC_USER_ID
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.resolveMdc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable
import java.util.*

@Service
class MyStickersBot(
        private val props: BotConfigProps,
        private val handlerFactory: HandlerFactory
) : TelegramLongPollingBot() {
    override fun onUpdateReceived(update: Update) {
        val handler = when {
            update.message.text == "/start" -> handlerFactory.startHandler()
            update.message.hasSticker() -> processSticker(update.message.sticker)
            else -> handlerFactory.unknownMessageHandler()
        }
        Mono.just(handler)
                .flatMap { it.handle(this, update) }
                .doOnEach {
                    if (it.isOnError) {
                        it.context.resolveMdc()
                        val t = it.throwable!!
                        if (t is TelegramApiRequestException) {
                            logger.error("Telegram api error: ${t.apiResponse}, code: ${t.errorCode}", t)
                        } else {
                            logger.error("Error occurred, message: ${t.message}", t)
                        }
                        sendErrorMessagesAsync(update.message.chatId, MDC.get(MDC_CALL_ID))
                        MDC.clear()
                    }
                }.subscriberContext {
                    it.put(MDC_CALL_ID, UUID.randomUUID().toString())
                            .put(MDC_USER_ID, update.message.chatId.toString())
                }.subscribe()
    }

    private fun processSticker(sticker: Sticker): BaseHandler {
        return if (sticker.animated) {
            handlerFactory.animatedStickerHandler()
        } else {
            handlerFactory.normalStickerHandler()
        }
    }

    private fun sendErrorMessagesAsync(chatId: Long, callId: String) {
        GlobalScope.launch(MDCContext()) {
            try {
                executeAsync(SendMessage(props.serviceAccountId, "Error occurred, check call id $callId"))
                executeAsync(SendMessage(chatId, "Sorry, some error occurred :( Admin is already notified, the problem will be fixed soon"))
            } catch (e: Exception) {
                logger.error("Unrecoverable error, message: ${e.message}", e)
            }
        }
    }

    override fun getBotUsername(): String = props.username

    override fun getBotToken(): String = props.token

    companion object : Loggable
}
