package com.vdsirotkin.telegram.mystickersbot.service

import com.vdsirotkin.dashbot.client.TrackClient
import com.vdsirotkin.dashbot.dto.TrackRequest
import com.vdsirotkin.dashbot.dto.track.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.bodyToMono
import ru.sokomishalov.commons.core.log.Loggable
import ru.sokomishalov.commons.core.reactor.await
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import javax.annotation.PostConstruct

@Service
class MetricsService(
        @Value("\${dashbot.enabled}") private val enabled: Boolean,
        private val trackClient: TrackClient
) {

    private val executor = Executors.newFixedThreadPool(2)
    private val incomingQueue: LinkedBlockingDeque<TrackRequest> = LinkedBlockingDeque()
    private val outgoingQueue: LinkedBlockingDeque<TrackRequest> = LinkedBlockingDeque()

    fun trackIncoming(chatId: Long, action: String, text: String = "placeholder") {
        incomingQueue.offer(TrackRequest(text, chatId.toString(), Intent(action)))
    }

    fun trackOutgoing(chatId: Long, action: String, text: String = "placeholder") {
        outgoingQueue.offer(TrackRequest(text, chatId.toString(), Intent(action)))
    }

    @PostConstruct
    fun init()  {
        if (!enabled) return
        val scope = CoroutineScope(executor.asCoroutineDispatcher())
        scope.launch {
            while (this.isActive) {
                val request = incomingQueue.take()
                try {
                    val response = trackClient.incoming(request)
                    logger.info("Status: ${response.statusCode()}, response: ${response.bodyToMono<String>().await()}")
                } catch (e: Exception) {
                    logger.error(e.message, e)
                }
            }
        }
        scope.launch(context = executor.asCoroutineDispatcher()) {
            while (this.isActive) {
                val request = outgoingQueue.take()
                try {
                    val response = trackClient.outgoing(request)
                    logger.info("Status: ${response.statusCode()}, response: ${response.bodyToMono<String>().await()}")
                } catch (e: Exception) {
                    logger.error(e.message, e)
                }
            }
        }
    }

    companion object : Loggable

}
