package com.vdsirotkin.telegram.mystickersbot.dao

import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import ru.sokomishalov.commons.core.reactor.await
import ru.sokomishalov.commons.core.reactor.awaitStrict
import ru.sokomishalov.commons.core.reactor.awaitUnit

@Service
class StickerDAO(
        private val template: ReactiveMongoTemplate
) {

    suspend fun userRegistered(userId: Long): Boolean = template.exists(Query.query(Criteria.where("_id").`is`(userId.toString())), UserEntity::class.java).awaitStrict()

    suspend fun saveUserPacks(userId: Long, normalStickerName: String, animatedStickerName: String) {
        return template.save(UserEntity(userId.toString(), normalStickerName, animatedStickerName))
                .awaitUnit()
    }

    suspend fun getUserEntity(userId: Long): UserEntity {
        return template.findById<UserEntity>(userId.toString())
                .awaitStrict()
    }

    suspend fun getAllUserIds(): List<Long> {
        return template.findAll(UserEntity::class.java)
                .map { it.userId.toLong() }
                .await()
    }

    suspend fun setCreatedStatus(chatId: Long, normalStickerCreated: Boolean? = null, animatedStickerCreated: Boolean? = null) {
        return template.findById<UserEntity>(chatId.toString())
                .map { entity ->
                    normalStickerCreated?.let { entity.normalPackCreated = it }
                    animatedStickerCreated?.let { entity.animatedPackCreated = it }
                    entity
                }
                .flatMap { template.save(it) }
                .awaitUnit()
    }

    suspend fun countUsers(): Int {
        return template.count(Query(), UserEntity::class.java).await()?.toInt() ?: 0
    }

}
