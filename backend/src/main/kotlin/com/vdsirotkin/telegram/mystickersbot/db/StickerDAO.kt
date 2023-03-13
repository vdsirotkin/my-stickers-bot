package com.vdsirotkin.telegram.mystickersbot.db

import com.pengrad.telegrambot.model.Sticker
import com.vdsirotkin.telegram.mystickersbot.db.entity.USER_ENTITY_COLLECTION_NAME
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity.*
import com.vdsirotkin.telegram.mystickersbot.db.entity.getPackName
import com.vdsirotkin.telegram.mystickersbot.db.entity.getPackSet
import com.vdsirotkin.telegram.mystickersbot.dto.StickerMeta
import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import com.vdsirotkin.telegram.mystickersbot.dto.packType
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.springframework.beans.factory.annotation.Qualifier
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
    private val template: ReactiveMongoTemplate,
    @Qualifier("dbRetry") private val retry: Retry,
) {

    suspend fun stickerExists(
        entity: UserEntity,
        sticker: Sticker
    ): Boolean {
        val packType = sticker.packType()
        val stickerPackContains = entity
            .getPackSet(packType)
            .firstOrNull { it.fileId == sticker.fileUniqueId() } != null
        val thisBotSticker = sticker.setName() == entity.getPackName(packType)
        return stickerPackContains || thisBotSticker
    }

    suspend fun userRegistered(userId: Long): Boolean = template.exists(Query.query(Criteria.where("_id").`is`(userId.toString())), UserEntity::class.java).awaitStrict()

    suspend fun saveUserPacks(userId: Long, normalStickerName: String, animatedStickerName: String, vidPackName: String) {
        return template.save(UserEntity(userId.toString(), normalStickerName, animatedStickerName, vidPackName))
            .awaitUnit()
    }

    suspend fun saveVideoPackName(userId: String, vidPackName: String): UserEntity {
        return retry.executeSuspendFunction {
            template.findById(userId, UserEntity::class.java).awaitStrict()
                .copy(videoPackName = vidPackName)
                .let { template.save(it).awaitStrict() }
        }
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
        retry.executeSuspendFunction {
            template.findById<UserEntity>(chatId.toString())
                .doOnNext {
                    // legacy
                    when (packType) {
                        StickerPackType.NORMAL -> it.normalPackCreated = true
                        StickerPackType.ANIMATED -> it.animatedPackCreated = true
                        StickerPackType.VIDEO -> it.videoPackCreated = true
                    }

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
                .await()
        }
    }

    suspend fun countUsers(): Int {
        return template.count(Query(), UserEntity::class.java).await()?.toInt() ?: 0
    }

    suspend fun saveSticker(chatId: Long, sticker: StickerMeta) {
        return retry.executeSuspendFunction {
            template.findById(chatId.toString(), UserEntity::class.java)
                .doOnNext {
                    val stickerInfo = StickerInfo(sticker.fileUniqueId, sticker.fileId)
                    // legacy
                    val set = it.getPackSet(sticker.type)
                    set.add(stickerInfo)

                    // new. assume that for now we have only one set of each type
                    val stickerSet = when (sticker.type) {
                        StickerPackType.NORMAL -> it.stickerSets.filterIsInstance<StaticStickerSet>()
                        StickerPackType.ANIMATED -> it.stickerSets.filterIsInstance<AnimatedStickerSet>()
                        StickerPackType.VIDEO -> it.stickerSets.filterIsInstance<VideoStickerSet>()
                    }
                    stickerSet.forEach { it.stickerSet.add(stickerInfo) }
                }
                .flatMap { template.save(it) }
                .awaitUnit()
        }
    }

    suspend fun saveUserLanguage(chatId: Long, selectedLanguage: String) {
        retry.executeSuspendFunction {
            template.findById(chatId.toString(), UserEntity::class.java)
                .map {
                    it.copy(language = selectedLanguage)
                }
                .flatMap { template.save(it) }
                .awaitUnit()
        }
    }

    suspend fun deleteSticker(chatId: Long, fileId: String, packType: StickerPackType) {
        retry.executeSuspendFunction {
            template.findById(chatId.toString(), UserEntity::class.java)
                .doOnNext {
                    // legacy
                    val set = it.getPackSet(packType)
                    set.removeIf { it.fileId == fileId }

                    // new
                    val stickerSet = when (packType) {
                        StickerPackType.NORMAL -> it.stickerSets.filterIsInstance<StaticStickerSet>()
                        StickerPackType.ANIMATED -> it.stickerSets.filterIsInstance<AnimatedStickerSet>()
                        StickerPackType.VIDEO -> it.stickerSets.filterIsInstance<VideoStickerSet>()
                    }
                    stickerSet.forEach { it.stickerSet.removeIf { it.fileId == fileId } }
                }
                .flatMap { template.save(it) }
                .awaitUnit()
        }
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
