package com.vdsirotkin.telegram.mystickersbot.db

import com.pengrad.telegrambot.model.Sticker
import com.vdsirotkin.telegram.mystickersbot.db.entity.USER_ENTITY_COLLECTION_NAME
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity.*
import com.vdsirotkin.telegram.mystickersbot.db.entity.getPackName
import com.vdsirotkin.telegram.mystickersbot.db.entity.getPackSets
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import com.vdsirotkin.telegram.mystickersbot.dto.packType
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.springframework.dao.DataAccessException
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import ru.sokomishalov.commons.core.log.Loggable
import ru.sokomishalov.commons.core.reactor.await
import ru.sokomishalov.commons.core.reactor.awaitStrict
import ru.sokomishalov.commons.core.reactor.awaitUnit
import java.time.Duration

@Repository
class StickerDAO(
    private val template: ReactiveMongoTemplate,
) {

    suspend fun stickerExists(
        entity: UserEntity,
        sticker: Sticker
    ): Boolean {
        val packType = sticker.packType()
        val stickerPackContains = entity
            .getPackSets(packType)
            .flatMap { it.stickerSet }
            .firstOrNull { it.originalStickerFileUniqueId == sticker.fileUniqueId() } != null
        val thisBotSticker = sticker.setName() == entity.getPackName(packType)
        return stickerPackContains || thisBotSticker
    }

    suspend fun userRegistered(userId: Long): Boolean = template.exists(Query.query(Criteria.where("_id").`is`(userId.toString())), UserEntity::class.java).awaitStrict()

    suspend fun saveUserPacks(userId: Long, normalStickerName: String, animatedStickerName: String, vidPackName: String) {
        return template.save(UserEntity(userId.toString(), normalStickerName, animatedStickerName, vidPackName))
            .awaitUnit()
    }

    suspend fun saveVideoPackName(userId: String, vidPackName: String): UserEntity {
        return template.findById(userId, UserEntity::class.java)
            .doOnNext { it.videoPackName = vidPackName }
            .flatMap { template.save(it) }
            .retryOnOptimisticLock()
            .awaitStrict()
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

    suspend fun createSet(chatId: Long, packType: StickerPackType, packName: String) {
        template.findById<UserEntity>(chatId.toString())
            .doOnNext {
                // new
                if (it.stickerSets.find { it.packName == packName } == null) {
                    val set = when (packType) {
                        StickerPackType.NORMAL -> StaticStickerSet(packName)
                        StickerPackType.ANIMATED -> AnimatedStickerSet(packName)
                        StickerPackType.VIDEO -> VideoStickerSet(packName)
                    }
                    it.stickerSets.add(set)
                }
            }
            .flatMap { template.save(it) }
            .retryOnOptimisticLock()
            .await()
    }

    suspend fun createEmojiSet(chatId: Long, packType: StickerPackType, packName: String) {
        template.findById<UserEntity>(chatId.toString())
            .doOnNext {
                // new
                if (it.emojiSets.find { it.packName == packName } == null) {
                    val set = when (packType) {
                        StickerPackType.NORMAL -> StaticEmojiSet(packName)
                        StickerPackType.ANIMATED -> AnimatedEmojiSet(packName)
                        StickerPackType.VIDEO -> VideoEmojiSet(packName)
                    }
                    it.emojiSets.add(set)
                }
            }
            .flatMap { template.save(it) }
            .retryOnOptimisticLock()
            .await()
    }

    suspend fun countUsers(): Int {
        return template.count(Query(), UserEntity::class.java).await()?.toInt() ?: 0
    }

    suspend fun saveSticker(chatId: Long, sticker: StickerMeta, createdStickerUniqueFileId: String) {
        return template.findById(chatId.toString(), UserEntity::class.java)
            .doOnNext {
                val stickerInfo = StickerInfo(sticker.fileUniqueId, createdStickerUniqueFileId)
                // assume that for now we have only one set of each type
                val stickerSet = when (sticker.type) {
                    StickerPackType.NORMAL -> it.stickerSets.filterIsInstance<StaticStickerSet>()
                    StickerPackType.ANIMATED -> it.stickerSets.filterIsInstance<AnimatedStickerSet>()
                    StickerPackType.VIDEO -> it.stickerSets.filterIsInstance<VideoStickerSet>()
                }
                stickerSet.forEach { it.stickerSet.add(stickerInfo) }
            }
            .flatMap { template.save(it) }
            .retryOnOptimisticLock()
            .awaitUnit()
    }

    suspend fun saveUserLanguage(chatId: Long, selectedLanguage: String) {
        template.findById(chatId.toString(), UserEntity::class.java)
            .map {
                it.copy(language = selectedLanguage)
            }
            .flatMap { template.save(it) }
            .retryOnOptimisticLock()
            .awaitUnit()
    }

    suspend fun deleteSticker(chatId: Long, fileId: String, packType: StickerPackType) {
        template.findById(chatId.toString(), UserEntity::class.java)
            .doOnNext {
                // new
                val stickerSet = when (packType) {
                    StickerPackType.NORMAL -> it.stickerSets.filterIsInstance<StaticStickerSet>()
                    StickerPackType.ANIMATED -> it.stickerSets.filterIsInstance<AnimatedStickerSet>()
                    StickerPackType.VIDEO -> it.stickerSets.filterIsInstance<VideoStickerSet>()
                }
                stickerSet.forEach { it.stickerSet.removeIf { it.createdStickerFileUniqueId == fileId } }
            }
            .flatMap { template.save(it) }
            .retryOnOptimisticLock()
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

    private fun <T> Mono<T>.retryOnOptimisticLock(): Mono<T> {
        return this.retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500))
            .filter { it is DataAccessException }
            .doBeforeRetry { logger.info("Retrying on optimistic lock exception") }
        )
    }

    companion object : Loggable
}
