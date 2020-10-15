package com.vdsirotkin.telegram.mystickersbot.db

import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobStatus
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserStatus
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.remove
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.sokomishalov.commons.core.common.unit
import ru.sokomishalov.commons.core.log.Loggable
import ru.sokomishalov.commons.core.reactor.await
import ru.sokomishalov.commons.core.reactor.awaitStrict
import java.util.*

@DataMongoTest
@ExtendWith(SpringExtension::class)
internal class BatchJobDAOTest {

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    lateinit var batchJobDAO: BatchJobDAO

    lateinit var jobId: String

    @BeforeEach
    fun init() {
        batchJobDAO = BatchJobDAO(template)
        template.remove<BatchJobEntity>(Query()).block()

        jobId = UUID.randomUUID().toString()
        val entity = BatchJobEntity(jobId, "test", "test", testData)
        template.save(entity).block()
    }

    @Test
    @DisplayName("test find all jobs")
    fun testFindAllJobs() = runBlocking {
        val allJobs = batchJobDAO.findAllJobs()
        assertThat(allJobs.size).isEqualTo(1)
    }.unit()

    @Test
    @DisplayName("test find unfinished jobs")
    fun testFindUnfinishedJobs() = runBlocking {
        template.save(BatchJobEntity(UUID.randomUUID().toString(), "test", "test", mutableListOf(), finished = true)).await()

        val overallSize = template.count(Query(), BatchJobEntity::class.java).await()
        val unfinishedJobs = batchJobDAO.findUnfinishedJobs()
        val size = unfinishedJobs.size

        assertThat(overallSize).isEqualTo(2)
        assertThat(size).isEqualTo(1)
        assertThat(unfinishedJobs.first()).isEqualTo(jobId)
    }.unit()

    @Test
    @DisplayName("test find unprocessed users batch")
    fun testFindUnprocessedUsersBatch() = runBlocking {
        val list = batchJobDAO.findUnprocessedUsersBatch(jobId)
        assertThat(list.size).isEqualTo(30)
        assertThat(list.filter { it.status == BatchJobStatus.NOT_STARTED }.size).isEqualTo(30)
    }.unit()

    @Test
    @DisplayName("test update processed statuses")
    fun testUpdateProcessedStatuses() = runBlocking {
        batchJobDAO.updateProcessedStatus(jobId, listOf(
                UserStatus(1, BatchJobStatus.SUCCESS),
                UserStatus(2, BatchJobStatus.SUCCESS),
                UserStatus(3, BatchJobStatus.SUCCESS),
                UserStatus(4, BatchJobStatus.SUCCESS)
        ))

        val entity = template.findById<BatchJobEntity>(jobId).awaitStrict()
        assertThat(entity.userStatus.filter { it.status == BatchJobStatus.SUCCESS }.size).isEqualTo(12)
    }.unit()

    @Test
    @DisplayName("test batch stats")
    fun testBatchStats() = runBlocking {
        val stats = batchJobDAO.getJobStats(jobId)

        assertThat(stats.overallCount).isEqualTo(52)
        assertThat(stats.processedCount).isEqualTo(9)
        assertThat(stats.successCount).isEqualTo(8)
    }.unit()

    private val testData = mutableListOf(
            UserStatus(1, BatchJobStatus.NOT_STARTED),
            UserStatus(2, BatchJobStatus.NOT_STARTED),
            UserStatus(3, BatchJobStatus.NOT_STARTED),
            UserStatus(4, BatchJobStatus.NOT_STARTED),
            UserStatus(5, BatchJobStatus.NOT_STARTED),
            UserStatus(6, BatchJobStatus.NOT_STARTED),
            UserStatus(7, BatchJobStatus.NOT_STARTED),
            UserStatus(8, BatchJobStatus.NOT_STARTED),
            UserStatus(9, BatchJobStatus.NOT_STARTED),
            UserStatus(10, BatchJobStatus.NOT_STARTED),
            UserStatus(11, BatchJobStatus.NOT_STARTED),
            UserStatus(12, BatchJobStatus.NOT_STARTED),
            UserStatus(13, BatchJobStatus.NOT_STARTED),
            UserStatus(14, BatchJobStatus.SUCCESS),
            UserStatus(15, BatchJobStatus.SUCCESS),
            UserStatus(16, BatchJobStatus.SUCCESS),
            UserStatus(17, BatchJobStatus.SUCCESS),
            UserStatus(18, BatchJobStatus.SUCCESS),
            UserStatus(19, BatchJobStatus.SUCCESS),
            UserStatus(20, BatchJobStatus.SUCCESS),
            UserStatus(21, BatchJobStatus.SUCCESS),
            UserStatus(22, BatchJobStatus.NOT_STARTED),
            UserStatus(23, BatchJobStatus.NOT_STARTED),
            UserStatus(24, BatchJobStatus.NOT_STARTED),
            UserStatus(25, BatchJobStatus.NOT_STARTED),
            UserStatus(26, BatchJobStatus.NOT_STARTED),
            UserStatus(27, BatchJobStatus.NOT_STARTED),
            UserStatus(28, BatchJobStatus.NOT_STARTED),
            UserStatus(29, BatchJobStatus.NOT_STARTED),
            UserStatus(30, BatchJobStatus.NOT_STARTED),
            UserStatus(31, BatchJobStatus.NOT_STARTED),
            UserStatus(32, BatchJobStatus.NOT_STARTED),
            UserStatus(33, BatchJobStatus.NOT_STARTED),
            UserStatus(34, BatchJobStatus.NOT_STARTED),
            UserStatus(35, BatchJobStatus.NOT_STARTED),
            UserStatus(36, BatchJobStatus.NOT_STARTED),
            UserStatus(37, BatchJobStatus.NOT_STARTED),
            UserStatus(38, BatchJobStatus.NOT_STARTED),
            UserStatus(39, BatchJobStatus.NOT_STARTED),
            UserStatus(40, BatchJobStatus.NOT_STARTED),
            UserStatus(41, BatchJobStatus.NOT_STARTED),
            UserStatus(42, BatchJobStatus.NOT_STARTED),
            UserStatus(43, BatchJobStatus.NOT_STARTED),
            UserStatus(44, BatchJobStatus.NOT_STARTED),
            UserStatus(45, BatchJobStatus.NOT_STARTED),
            UserStatus(46, BatchJobStatus.NOT_STARTED),
            UserStatus(47, BatchJobStatus.NOT_STARTED),
            UserStatus(48, BatchJobStatus.NOT_STARTED),
            UserStatus(49, BatchJobStatus.NOT_STARTED),
            UserStatus(50, BatchJobStatus.NOT_STARTED),
            UserStatus(51, BatchJobStatus.ERROR),
            UserStatus(52, BatchJobStatus.NOT_STARTED)
    )

    companion object : Loggable

}
