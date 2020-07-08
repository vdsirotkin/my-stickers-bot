package com.vdsirotkin.telegram.mystickersbot.batch

import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.db.BatchJobDAO
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobStatus
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserStatus
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.mockk.every
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import ru.sokomishalov.commons.core.common.unit
import ru.sokomishalov.commons.core.reactor.awaitStrict
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

@DataMongoTest
@ExtendWith(SpringExtension::class)
class JobProcessorIT {

    lateinit var jobProcessor: JobProcessor
    lateinit var jobId: String

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    private val jobCount = 160

    @BeforeEach
    fun setUp() {
        template.remove(Query(), BatchJobEntity::class.java).block()
        val random = Random(currentTimeMillis())
        val bot = mockkClass(MyStickersBot::class) {
            every { retry } returns Retry.of("", RetryConfig.custom<Any>().retryOnException { false }.build())
            every { rateLimiter } returns RateLimiter.ofDefaults("")
            every {
                executeAsync(any<SendMessage>(), any<SentCallback<Message>>())
            } answers {
                val int = random.nextInt(1, 100)
                if (int > 50) {
                    (secondArg() as SentCallback<Message>).onException(null, TelegramApiException("Some error occured"))
                } else {
                    (secondArg() as SentCallback<Message>).onResult(null, Message())
                }
            }
        }
        jobProcessor = JobProcessor(JobManager(BatchJobDAO(template), StickerDAO(template)), bot)
        val userStatus = generateSequence { UserStatus(ThreadLocalRandom.current().nextLong(100000, 1000000), BatchJobStatus.NOT_STARTED) }.take(jobCount).toMutableList()
        template.save(BatchJobEntity(UUID.randomUUID().toString().apply { jobId = this }, "kek", "prived medved", userStatus)).block()
    }

    @Test
    fun `test job processing`() = runBlocking {

        jobProcessor.startJob(jobId).join()

        val entity: BatchJobEntity = template.findById(jobId, BatchJobEntity::class.java).awaitStrict()
        assertThat(entity.userStatus.size).isEqualTo(jobCount)
        assertThat(entity.userStatus.filter { it.status == BatchJobStatus.NOT_STARTED }.size).isEqualTo(0)
        assertThat(entity.userStatus.filter { it.status == BatchJobStatus.SUCCESS }.size).isGreaterThan(10)
        assertThat(entity.userStatus.filter { it.status == BatchJobStatus.ERROR }.size).isGreaterThan(10)
        assertThat(entity.finished).isEqualTo(true)
        entity
    }.unit()
}
