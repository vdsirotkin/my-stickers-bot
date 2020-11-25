package com.vdsirotkin.telegram.mystickersbot.batch

import com.pengrad.telegrambot.TelegramException
import com.pengrad.telegrambot.model.request.ParseMode
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobStatus
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserStatus
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.util.MDC_USER_ID
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.MDC
import org.springframework.stereotype.Service
import ru.sokomishalov.commons.core.collections.aMap
import ru.sokomishalov.commons.core.log.Loggable

@Service
class JobProcessor(
        private val jobManager: JobManager,
        private val bot: MyStickersBot
) {

    fun startJob(jobId: String) = GlobalScope.launch {
        val text = jobManager.getJobText(jobId)
        var finished = false
        while (!finished) {
            val nextBatch = jobManager.getNextBatch(jobId)
            if (nextBatch.isEmpty()) {
                finished = true
                jobManager.stopJob(jobId)
            }
            nextBatch.aMap { chatId: Long ->
                val result = runCatching {
                    bot.executeAsync(SendMessageWithAction(chatId, text, "BATCH_JOB").parseMode(ParseMode.HTML))
                }
                if (result.isSuccess) {
                    chatId to BatchJobStatus.SUCCESS
                } else {
                    val e = result.exceptionOrNull()!!
                    MDC.put(MDC_USER_ID, chatId.toString())
                    logException(e, chatId)
                    MDC.clear()
                    chatId to BatchJobStatus.ERROR
                }
            }.map { UserStatus(it.first, it.second) }
                    .also { jobManager.finishBatch(jobId, it) }
        }
    }

    private fun logException(e: Throwable, it: Long) {
        if (e is TelegramException) {
            logger.warn("Can't send to user $it, message: ${e.message}, result: ${e.response()}, errorCode: ${e.response().errorCode()}")
        } else {
            logger.warn("Can't send to user $it, message: ${e.message}", e)
        }
    }

    companion object : Loggable

}
