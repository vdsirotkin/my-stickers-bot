package com.vdsirotkin.telegram.mystickersbot.dao

import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker
import ru.sokomishalov.commons.core.reactor.await
import ru.sokomishalov.commons.core.reactor.awaitStrict
import ru.sokomishalov.commons.core.reactor.awaitUnit

@Service
class StickerDAO(
        private val template: ReactiveMongoTemplate
) {

    suspend fun stickerExists(userId: Long, stickerId: String, isAnimated: Boolean): Boolean {
        return template.findById(userId.toString(), UserEntity::class.java).awaitStrict().let {
            if (isAnimated) it.animatedPackSet else it.normalPackSet
        }.firstOrNull { it.fileId == stickerId }?.let { true } ?: false
    }

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

    suspend fun saveSticker(chatId: Long, sticker: Sticker, isAnimated: Boolean) {
        return template.findById(chatId.toString(), UserEntity::class.java)
                .doOnNext {
                    val set = if(isAnimated) it.animatedPackSet else it.normalPackSet
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

}
