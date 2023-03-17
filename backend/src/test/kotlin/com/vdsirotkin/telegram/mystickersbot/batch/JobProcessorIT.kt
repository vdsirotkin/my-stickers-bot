package com.vdsirotkin.telegram.mystickersbot.batch

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.db.BatchJobDAO
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobStatus
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserStatus
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.commons.util.ReflectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.sokomishalov.commons.core.common.unit
import ru.sokomishalov.commons.core.reactor.awaitStrict
import java.io.IOException
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

@DataMongoTest
@ExtendWith(SpringExtension::class, MockKExtension::class)
class JobProcessorIT(
    @MockK private val stickerDAO: StickerDAO
) {

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
                execute(any<SendMessage>(), any<Callback<SendMessage, SendResponse>>())
            } answers {
                val int = random.nextInt(1, 100)
                if (int > 50) {
                    (secondArg() as Callback<SendMessage, SendResponse>).onFailure(null, IOException())
                } else {
                    (secondArg() as Callback<SendMessage, SendResponse>).onResponse(null, ReflectionUtils.newInstance(SendResponse::class.java))
                }
            }
        }
        coEvery { stickerDAO.getUserEntity(any<Long>()) } returns UserEntity("", "", "")
        jobProcessor = JobProcessor(JobManager(BatchJobDAO(template), StickerDAO(template)), stickerDAO, bot)
        val userStatus = generateSequence { UserStatus(ThreadLocalRandom.current().nextLong(100000, 1000000), BatchJobStatus.NOT_STARTED) }.take(jobCount).toMutableList()
        template.save(BatchJobEntity(UUID.randomUUID().toString().also { jobId = it }, "kek", "prived medved", userStatus)).block()
    }

    @Test
    fun testJobProcessing() = runBlocking {

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
