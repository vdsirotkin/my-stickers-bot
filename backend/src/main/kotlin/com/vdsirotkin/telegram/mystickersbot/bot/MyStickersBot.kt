package com.vdsirotkin.telegram.mystickersbot.bot

import com.pengrad.telegrambot.*
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Sticker
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import com.vdsirotkin.telegram.mystickersbot.dto.HandlerState
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.exception.HandlerException
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.handler.HandlerFactory
import com.vdsirotkin.telegram.mystickersbot.handler.StatefulHandler
import com.vdsirotkin.telegram.mystickersbot.service.LocalizedMessageSourceProvider
import com.vdsirotkin.telegram.mystickersbot.service.MetricsService
import com.vdsirotkin.telegram.mystickersbot.util.*
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.kotlin.core.publisher.toMono
import ru.sokomishalov.commons.core.log.Loggable
import java.util.*
import javax.annotation.PostConstruct
import kotlin.reflect.KClass

@Service
class MyStickersBot(
    private val props: BotConfigProps,
    private val handlerFactory: HandlerFactory,
    private val messageSourceProvider: LocalizedMessageSourceProvider,
    private val metricsService: MetricsService,
    @Qualifier("telegramApiRetry") val retry: Retry,
    val rateLimiter: RateLimiter
) : TelegramBot(props.token) {

    private val handlerStateMap: MutableMap<Long, HandlerState<*>> = mutableMapOf()

    @PostConstruct
    fun init() {
        this.setUpdatesListener {
            it.filter {
                it.message() != null || it.callbackQuery() != null
            }.forEach {
                try {
                    onUpdateReceived(it)
                } catch (e: Exception) {
                    logger.error("Error while processing update: $it", e)
                }
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    fun onUpdateReceived(update: Update) {
        if (isGroup(update)) {
            handleGroup(update)
            return
        }
        val chatId = determineChatId(update)
        if (update.message()?.text() == "/cancel") {
            handleCancel(chatId)
            return
        }
        val handler: BaseHandler = when {
            update.callbackQuery() != null -> when {
                update.callbackQuery().data().contains(setLanguageCommandPrefix) -> handlerFactory.setLanguageHandler
                update.callbackQuery().data().startsWith(chooseEmoji) -> processEmojiQuery(chatId)
                else -> handlerFactory.unknownMessageHandler
            }
            handlerStateMap.containsKey(chatId) -> prepareStatefulHandler(chatId)
            update.message()?.text() in arrayOf("/start", "/help") -> handlerFactory.startHandler
            update.message()?.text() == "/language" -> handlerFactory.languageHandler
            update.message()?.text() == "/delete" -> handlerFactory.deleteHandler
            update.message()?.text() == "/download" -> handlerFactory.downloadHandler
            update.message()?.sticker() != null -> processSticker(update.message().sticker())
            update.message()?.photo() != null -> handlerFactory.photoHandler
            update.message()?.animation() != null -> handlerFactory.animationHandler
            update.message()?.document() != null -> handlerFactory.documentHandler
            else -> handlerFactory.unknownMessageHandler
        }

        metricsService.trackIncoming(chatId, handler.action)

        handler.toMono()
            .flatMap { it.handle(this, update) }
            .doOnNext {
                if (it is StatefulHandler<*>) {
                    if (it.getState().finished) {
                        handlerStateMap.remove(chatId)
                    } else {
                        handlerStateMap[chatId] = it.getState()
                    }
                }
            }
            .doOnEach {
                if (!it.isOnError) return@doOnEach

                it.contextView.resolveMdc()
                val t = it.throwable!!

                if (t is HandlerException) {
                    logException(t.parent)
                    if (t.handler is StatefulHandler<*>) {
                        handlerStateMap.remove(chatId)
                    }
                } else {
                    logException(t)
                }
                sendErrorMessagesAsync(chatId, MDC.get(MDC_CALL_ID))
                MDC.clear()
            }.contextWrite {
                it.put(MDC_CALL_ID, UUID.randomUUID().toString())
                    .put(MDC_USER_ID, chatId.toString())
            }.subscribe()
    }

    private fun handleGroup(update: Update) {
        GlobalScope.launch {
            executeAsync(SendMessage(update.message().chat().id(), GROUP_MESSAGE))
        }
    }

    private fun isGroup(update: Update): Boolean {
        val type = update.message()?.chat()?.type() ?: return false
        return when (type) {
            Chat.Type.group, Chat.Type.supergroup, Chat.Type.channel -> true
            else -> false
        }
    }

    private fun processEmojiQuery(chatId: Long): BaseHandler {
        return if (handlerStateMap.containsKey(chatId)) {
            prepareStatefulHandler(chatId)
        } else {
            handlerFactory.unknownMessageHandler
        }
    }

    private fun logException(t: Throwable) {
        if (t is TelegramException) {
            logger.error("Telegram api error: ${t.response()}, code: ${t.response().errorCode()}", t)
        } else {
            logger.error("Error occurred, message: ${t.message}", t)
        }
    }

    private fun handleCancel(chatId: Long) {
        if (handlerStateMap.containsKey(chatId)) {
            val state = handlerStateMap.remove(chatId)!!
            val handler = prepareStatefulHandler(state)
            GlobalScope.launch {
                handler.cancel(this@MyStickersBot, chatId)
                val messageSource = messageSourceProvider.getMessageSource(chatId)
                executeAsync(SendMessageWithAction(chatId, messageSource["cancel.success"], "CANCEL"))
            }
        } else {
            GlobalScope.launch {
                val messageSource = messageSourceProvider.getMessageSource(chatId)
                executeAsync(SendMessageWithAction(chatId, messageSource["cancel.nothing"], "NOTHING_TO_CANCEL"))
            }
        }
    }

    private fun prepareStatefulHandler(chatId: Long): BaseHandler {
        val state = handlerStateMap[chatId]!!
        return prepareStatefulHandler(state)
    }

    @Suppress("UNCHECKED_CAST")
    private fun prepareStatefulHandler(state: HandlerState<*>): StatefulHandler<Any> {
        return (handlerFactory.newHandler(Class.forName(state.handlerClass).kotlin as KClass<out BaseHandler>) as StatefulHandler<Any>).apply {
            setState(state as HandlerState<Any>)
        }
    }

    private fun processSticker(sticker: Sticker): BaseHandler {
        return if (sticker.isAnimated) {
            handlerFactory.animatedStickerHandler
        } else if (sticker.isVideo) {
            handlerFactory.videoStickerHandler
        } else {
            handlerFactory.normalStickerHandler
        }
    }

    private fun sendErrorMessagesAsync(chatId: Long, callId: String) {
        GlobalScope.launch(MDCContext()) {
            try {
                val messageSource = messageSourceProvider.getMessageSource(chatId)
                executeAsync(SendMessage(props.serviceAccountId, "Error occurred, check call id $callId"))
                executeAsync(SendMessageWithAction(chatId, messageSource["error"], "ERROR"))
            } catch (e: Exception) {
                logger.error("Unrecoverable error, message: ${e.message}", e)
            }
        }
    }

    override fun <T : BaseRequest<T, R>?, R : BaseResponse?> execute(request: T, callback: Callback<T, R>?): Cancellable {
        if (request is SendMessageWithAction) {
            metricsService.trackOutgoing(request.parameters["chat_id"]?.toString()?.toLong() ?: 0, request.action, request.parameters["text"]?.toString().orEmpty())
        }
        return super.execute(request, callback)
    }

    companion object : Loggable
}
