package com.vdsirotkin.telegram.mystickersbot.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

const val BATCH_JOB_COLLECTION_NAME = "batchJob"

@Document(collection = BATCH_JOB_COLLECTION_NAME)
data class BatchJobEntity(
        @field:Id
        val id: String,
        val name: String,
        val text: String,
        val userStatus: MutableList<UserStatus> = mutableListOf(),
        val textRu: String = "",
        val finished: Boolean = false
) {

}

data class UserStatus(
        val userId: Long,
        val status: BatchJobStatus
)

enum class BatchJobStatus {
    NOT_STARTED, SUCCESS, ERROR
}

