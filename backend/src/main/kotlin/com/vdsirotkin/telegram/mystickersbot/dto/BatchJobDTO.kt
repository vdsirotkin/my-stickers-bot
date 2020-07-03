package com.vdsirotkin.telegram.mystickersbot.dto

import com.vdsirotkin.telegram.mystickersbot.db.entity.BatchJobEntity

data class BatchJobDTO(
        val jobMeta: BatchJobEntity,
        val jobStats: BatchJobStats
)

data class BatchJobStats(
        val overallCount: Long,
        val processedCount: Long,
        val successCount: Long
)
