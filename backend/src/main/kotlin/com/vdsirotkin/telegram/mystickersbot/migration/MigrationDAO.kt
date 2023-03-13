package com.vdsirotkin.telegram.mystickersbot.migration

import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import ru.sokomishalov.commons.core.reactor.await

@Repository
class MigrationDAO(
    private val template: ReactiveMongoTemplate,
) {

    fun getUnmigratedBatch(size: Int): List<UserEntity> {
        return template.find<UserEntity>(
            Query.query(where(UserEntity::migrated).`is`(false))
                .limit(size))
            .collectList().block() ?: emptyList()
    }

    @Transactional
    fun updateOne(userEntity: UserEntity): Mono<UserEntity> {
        return template.save(userEntity)
    }

    fun countUsers(): Int {
        return template.count(Query(), UserEntity::class.java).block()?.toInt() ?: 0
    }
}
