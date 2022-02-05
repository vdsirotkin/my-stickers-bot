package com.vdsirotkin.telegram.mystickersbot.db

import com.pengrad.telegrambot.model.Sticker
import com.vdsirotkin.telegram.mystickersbot.db.entity.USER_ENTITY_COLLECTION_NAME
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import ru.sokomishalov.commons.core.reactor.await
import ru.sokomishalov.commons.core.reactor.awaitStrict
import ru.sokomishalov.commons.core.reactor.awaitUnit

@Repository
class StickerDAO(
        private val template: ReactiveMongoTemplate
) {

    suspend fun stickerExists(entity: UserEntity,
                              sticker: Sticker,
                              isAnimated: Boolean): Boolean {
        val stickerPackContains = entity.let {
            if (isAnimated) it.animatedPackSet else it.normalPackSet
        }.firstOrNull { it.fileId == sticker.fileUniqueId() }?.let { true } ?: false
        val thisBotSticker = sticker.setName() == if (isAnimated) entity.animatedPackName else entity.normalPackName
        return stickerPackContains || thisBotSticker
    }

    suspend fun videoStickerExists(entity: UserEntity,
                              sticker: Sticker): Boolean {
        val stickerPackContains = entity.videoPackSet.firstOrNull { it.fileId == sticker.fileUniqueId() }?.let { true } ?: false
        val thisBotSticker = sticker.setName() == entity.videoPackName
        return stickerPackContains || thisBotSticker
    }

    suspend fun userRegistered(userId: Long): Boolean = template.exists(Query.query(Criteria.where("_id").`is`(userId.toString())), UserEntity::class.java).awaitStrict()

    suspend fun saveUserPacks(userId: Long, normalStickerName: String, animatedStickerName: String, vidPackName: String) {
        return template.save(UserEntity(userId.toString(), normalStickerName, animatedStickerName, vidPackName))
                .awaitUnit()
    }

    suspend fun saveVideoPackName(userId: String, vidPackName: String): UserEntity {
        return template.findById(userId, UserEntity::class.java).awaitStrict()
            .copy(videoPackName = vidPackName)
            .let { template.save(it).awaitStrict() }
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

    suspend fun setCreatedStatus(chatId: Long, normalStickerCreated: Boolean? = null, animatedStickerCreated: Boolean? = null, videoPackCreated: Boolean? = null) {
        return template.findById<UserEntity>(chatId.toString())
                .map { entity ->
                    normalStickerCreated?.let { entity.normalPackCreated = it }
                    animatedStickerCreated?.let { entity.animatedPackCreated = it }
                    videoPackCreated?.let { entity.videoPackCreated = it }
                    entity
                }
                .flatMap { template.save(it) }
                .awaitUnit()
    }

    suspend fun countUsers(): Int {
        return template.count(Query(), UserEntity::class.java).await()?.toInt() ?: 0
    }

    suspend fun saveSticker(chatId: Long, sticker: StickerMeta, isAnimated: Boolean) {
        return template.findById(chatId.toString(), UserEntity::class.java)
                .doOnNext {
                    val set = if(isAnimated) it.animatedPackSet else it.normalPackSet
                    set.add(UserEntity.StickerInfo(sticker.fileUniqueId, sticker.fileId))
                }
                .flatMap { template.save(it) }
                .awaitUnit()
    }

    suspend fun saveVideoSticker(chatId: Long, sticker: StickerMeta) {
        return template.findById(chatId.toString(), UserEntity::class.java)
                .doOnNext {
                    val set = it.videoPackSet
                    set.add(UserEntity.StickerInfo(sticker.fileUniqueId, sticker.fileId))
                }
                .flatMap { template.save(it) }
                .awaitUnit()
    }

    suspend fun saveUserLanguage(chatId: Long, selectedLanguage: String) {
        template.findById(chatId.toString(), UserEntity::class.java)
                .map { it.copy(language = selectedLanguage) }
                .flatMap { template.save(it) }
                .awaitUnit()
    }

    suspend fun deleteSticker(chatId: Long, fileId: String, isAnimated: Boolean) {
        template.findById(chatId.toString(), UserEntity::class.java)
                .doOnNext {
                    val set = if(isAnimated) it.animatedPackSet else it.normalPackSet
                    set.removeIf { it.fileId == fileId }
                }
                .flatMap { template.save(it) }
                .awaitUnit()
    }

    suspend fun countNormalAddedStickers(): Long {
        return template.aggregate(
                Aggregation.newAggregation(
                        Aggregation.unwind("normalPackSet"),
                        Aggregation.replaceRoot("normalPackSet"),
                        Aggregation.count().`as`("count")
                ), USER_ENTITY_COLLECTION_NAME, BatchJobDAO.Count::class.java
        ).awaitFirstOrElse { BatchJobDAO.Count(0) }.count
    }

    suspend fun countAnimatedAddedStickers(): Long {
        return template.aggregate(
                Aggregation.newAggregation(
                        Aggregation.unwind("animatedPackSet"),
                        Aggregation.replaceRoot("animatedPackSet"),
                        Aggregation.count().`as`("count")
                ), USER_ENTITY_COLLECTION_NAME, BatchJobDAO.Count::class.java
        ).awaitFirstOrElse { BatchJobDAO.Count(0) }.count
    }

}
