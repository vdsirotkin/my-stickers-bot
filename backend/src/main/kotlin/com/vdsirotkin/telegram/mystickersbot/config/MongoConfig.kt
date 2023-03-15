package com.vdsirotkin.telegram.mystickersbot.config

import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.MongoConfigurationSupport

@Configuration
class MongoConfig : MongoConfigurationSupport() {

    override fun getInitialEntitySet(): MutableSet<Class<*>> {
        return mutableSetOf(UserEntity.VideoStickerSet::class.java, UserEntity.AnimatedStickerSet::class.java, UserEntity.StaticStickerSet::class.java)
    }

    override fun getDatabaseName(): String {
        return "test"
    }
}
