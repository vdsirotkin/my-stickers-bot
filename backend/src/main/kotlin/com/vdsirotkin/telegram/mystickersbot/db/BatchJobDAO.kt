package com.vdsirotkin.telegram.mystickersbot.db

import com.vdsirotkin.telegram.mystickersbot.db.entity.BATCH_JOB_COLLECTION_NAME
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobStatus
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserStatus
import com.vdsirotkin.telegram.mystickersbot.dto.BatchJobStats
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.Update.update
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.sokomishalov.commons.core.reactor.await
import ru.sokomishalov.commons.core.reactor.awaitStrict

@Repository
class BatchJobDAO(
        private val template: ReactiveMongoTemplate
) {

    suspend fun findAllJobs(): List<Pair<String, String>> {
        return template.find<BatchJobEntity>(Query().onlyMeta()).await().map { it.id to it.name }
    }

    suspend fun findUnfinishedJobs(): List<String> {
        return template.find<BatchJobEntity>(query(where(BatchJobEntity::finished).`is`(false)).onlyMeta()).await().map { it.id }
    }

    suspend fun save(batchJobEntity: BatchJobEntity): BatchJobEntity {
        return template.save(batchJobEntity).await()!!
    }

    suspend fun findUnprocessedUsersBatch(jobId: String): List<UserStatus> {
        return template.aggregate(
                newAggregation(
                        match(Criteria.where("_id").`is`(jobId)),
                        unwind("userStatus"),
                        replaceRoot("userStatus"),
                        match(where(UserStatus::status).`is`(BatchJobStatus.NOT_STARTED.name)),
                        limit(30)),
                BATCH_JOB_COLLECTION_NAME,
                UserStatus::class.java).await()
    }

    @Transactional
    suspend fun updateProcessedStatus(jobId: String, updatedStatuses: List<UserStatus>) {
        for (it in updatedStatuses) {
            template.updateFirst(
                    query(where(BatchJobEntity::id).`is`(jobId).and("userStatus.userId").`is`(it.userId)),
                    Update().set("userStatus.$.status", it.status),
                    BatchJobEntity::class.java
            ).await()
        }
    }

    suspend fun finishJobExplicitly(jobId: String) {
        template.updateFirst(query(where(BatchJobEntity::id).`is`(jobId)), update("finished", true), BatchJobEntity::class.java).await()
    }

    suspend fun getJobMeta(jobId: String): BatchJobEntity {
        return template.findOne(query(where(BatchJobEntity::id).`is`(jobId)).onlyMeta(), BatchJobEntity::class.java).awaitStrict()
    }

    suspend fun getJobStats(jobId: String): BatchJobStats {
        val overallCount = template.aggregate(newAggregation(
                match(Criteria.where("_id").`is`(jobId)),
                unwind("userStatus"),
                replaceRoot("userStatus"),
                count().`as`("count")
        ), BATCH_JOB_COLLECTION_NAME, Count::class.java).awaitFirstOrElse { Count(0) }.count
        val processedCount = template.aggregate(newAggregation(
                match(Criteria.where("_id").`is`(jobId)),
                unwind("userStatus"),
                replaceRoot("userStatus"),
                match(where(UserStatus::status).ne(BatchJobStatus.NOT_STARTED.name)),
                count().`as`("count")
        ), BATCH_JOB_COLLECTION_NAME, Count::class.java).awaitFirstOrElse { Count(0) }.count

        val successCount = template.aggregate(newAggregation(
                match(Criteria.where("_id").`is`(jobId)),
                unwind("userStatus"),
                replaceRoot("userStatus"),
                match(where(UserStatus::status).`is`(BatchJobStatus.SUCCESS.name)),
                count().`as`("count")
        ), BATCH_JOB_COLLECTION_NAME, Count::class.java).awaitFirstOrElse { Count(0) }.count

        return BatchJobStats(overallCount, processedCount, successCount)
    }

    private fun Query.onlyMeta(): Query {
        return this.apply {
            fields()
                    .include("_id")
                    .include("name")
                    .include("text")
                    .include("textRu")
                    .include("currentOffset")
                    .include("finished")
        }
    }

    data class Count(val count: Long)

}
