package com.vdsirotkin.telegram.mystickersbot.batch

import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobStatus
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserStatus
import com.vdsirotkin.telegram.mystickersbot.util.MDC_USER_ID
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
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
            nextBatch.map {
                async {
                    val result = runCatching {
                        bot.executeAsync(SendMessage(it, text).enableMarkdownV2(true))
                    }
                    if (result.isSuccess) {
                        it to BatchJobStatus.SUCCESS
                    } else {
                        val e = result.exceptionOrNull()!!
                        MDC.put(MDC_USER_ID, it.toString())
                        if (e is TelegramApiRequestException) {
                            logger.warn("Can't send to user $it, message: ${e.message}, result: ${e.apiResponse}, errorCode: ${e.errorCode}")
                        } else {
                            logger.warn("Can't send to user $it, message: ${e.message}", e)
                        }
                        MDC.clear()
                        it to BatchJobStatus.ERROR
                    }
                }
            }.awaitAll().map { UserStatus(it.first, it.second) }
                    .also { jobManager.finishBatch(jobId, it) }
        }
    }

    companion object : Loggable

}
