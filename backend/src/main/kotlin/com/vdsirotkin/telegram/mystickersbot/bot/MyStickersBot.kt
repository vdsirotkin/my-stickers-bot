package com.vdsirotkin.telegram.mystickersbot.bot

import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.HandlerFactory
import com.vdsirotkin.telegram.mystickersbot.service.LocalizedMessageSourceProvider
import com.vdsirotkin.telegram.mystickersbot.util.*
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
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
import reactor.kotlin.core.publisher.toMono
import ru.sokomishalov.commons.core.log.Loggable
import java.util.*

@Service
class MyStickersBot(
        private val props: BotConfigProps,
        private val handlerFactory: HandlerFactory,
        private val messageSourceProvider: LocalizedMessageSourceProvider,
        val retry: Retry,
        val rateLimiter: RateLimiter
) : TelegramLongPollingBot() {
    override fun onUpdateReceived(update: Update) {
        val handler: BaseHandler = when {
            update.message?.text == "/start" -> handlerFactory.startHandler()
            update.message?.text == "/language" -> handlerFactory.languageHandler()
            update.message?.hasSticker() == true -> processSticker(update.message.sticker)
            update.message?.hasPhoto() == true -> handlerFactory.photoHandler()
            update.message?.hasDocument() == true -> handlerFactory.documentHandler()
            update.hasCallbackQuery() -> when {
                update.callbackQuery.data.contains(setLanguageCommandPrefix) -> handlerFactory.setLanguageHandler()
                else -> handlerFactory.unknownMessageHandler()
            }
            else -> handlerFactory.unknownMessageHandler()
        }
        val chatId = when {
            update.hasMessage() -> update.message.chatId
            update.hasCallbackQuery() -> update.callbackQuery.from.id.toLong()
            else -> throw IllegalArgumentException("Unsupported message type")
        }
        handler.toMono()
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
                        sendErrorMessagesAsync(chatId, MDC.get(MDC_CALL_ID))
                        MDC.clear()
                    }
                }.subscriberContext {
                    it.put(MDC_CALL_ID, UUID.randomUUID().toString())
                            .put(MDC_USER_ID, chatId.toString())
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
                val messageSource = messageSourceProvider.getMessageSource(chatId)
                executeAsync(SendMessage(props.serviceAccountId, "Error occurred, check call id $callId"))
                executeAsync(SendMessage(chatId, messageSource.getMessage("error")))
            } catch (e: Exception) {
                logger.error("Unrecoverable error, message: ${e.message}", e)
            }
        }
    }

    override fun getBotUsername(): String = props.username

    override fun getBotToken(): String = props.token

    companion object : Loggable
}
