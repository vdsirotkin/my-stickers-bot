package com.vdsirotkin.telegram.mystickersbot.batch

import com.vdsirotkin.telegram.mystickersbot.db.BatchJobDAO
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobStatus
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserStatus
import com.vdsirotkin.telegram.mystickersbot.dto.BatchJobDTO
import org.springframework.stereotype.Service
import java.util.*

@Service
class JobManager(
        private val batchDao: BatchJobDAO,
        private val stickerDAO: StickerDAO
) {

    suspend fun createNewJob(name: String, text: String): BatchJobEntity {
        val userMap = stickerDAO.getAllUserIds().sorted().map { UserStatus(it, BatchJobStatus.NOT_STARTED) }
        return batchDao.save(BatchJobEntity(UUID.randomUUID().toString(), name, text, userMap.toMutableList()))
    }

    suspend fun getJobText(jobId: String): String {
        return batchDao.getJobMeta(jobId).text
    }

    suspend fun getJobMeta(jobId: String): BatchJobDTO {
        val meta = batchDao.getJobMeta(jobId)
        val stats = batchDao.getJobStats(jobId)
        return BatchJobDTO(meta, stats)
    }

    suspend fun getNextBatch(jobId: String): List<Long> {
        return batchDao.findUnprocessedUsersBatch(jobId).map { it.userId }
    }

    suspend fun isFinished(jobId: String): Boolean {
        return batchDao.getJobMeta(jobId).finished
    }

    suspend fun stopJob(jobId: String) {
        batchDao.finishJobExplicitly(jobId)
    }

    suspend fun finishBatch(jobId: String,
                            users: List<UserStatus>) {
        batchDao.updateProcessedStatus(jobId, users)
    }

    suspend fun listJobs(): List<Pair<String, String>> {
        return batchDao.findAllJobs()
    }

}
