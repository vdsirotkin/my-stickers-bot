package com.vdsirotkin.telegram.mystickersbot.db

import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import io.github.resilience4j.retry.Retry
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension

@DataMongoTest
@ExtendWith(SpringExtension::class)
class StickerDAOTest {

    @Autowired
    lateinit var template: ReactiveMongoTemplate

    lateinit var stickerDAO: StickerDAO

    val retry = spyk(Retry.ofDefaults("test"))

    @BeforeEach
    fun setup() {
        stickerDAO = StickerDAO(template, retry)

        template.remove(UserEntity::class.java).all().block()
    }

    @Test
    fun `save different types`() {
        template.save(
            UserEntity("123", "normalPackName", "animatedPackName", "videoPackName")
                .apply {
                    stickerSets.add(UserEntity.AnimatedStickerSet("asdf"))
                    stickerSets.add(UserEntity.AnimatedStickerSet("asdf2"))
                    stickerSets.add(UserEntity.AnimatedStickerSet("asdf3"))
                    stickerSets.add(UserEntity.StaticStickerSet("asdf321"))
                    stickerSets.add(UserEntity.VideoStickerSet("asdfrqwe"))
                }
        )
            .block()

        val userId = 123L
        val entity = runBlocking { stickerDAO.getUserEntity(userId) }
        Assertions.assertThat(entity.stickerSets).hasSize(5)
    }
}
