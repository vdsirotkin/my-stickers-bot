package com.vdsirotkin.telegram.mystickersbot.db.entity

import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

const val USER_ENTITY_COLLECTION_NAME = "userEntity"

@Document(collection = USER_ENTITY_COLLECTION_NAME)
data class UserEntity(
    @field:Id
    val userId: String,
    @Deprecated(message = "legacy")
    val normalPackName: String,
    @Deprecated(message = "legacy")
    val animatedPackName: String,
    @Deprecated(message = "legacy")
    val videoPackName: String = "",
    @Deprecated(message = "legacy")
    var normalPackCreated: Boolean = false,
    @Deprecated(message = "legacy")
    var animatedPackCreated: Boolean = false,
    @Deprecated(message = "legacy")
    var videoPackCreated: Boolean = false,
    @Deprecated(message = "legacy")
    val normalPackSet: MutableSet<StickerInfo> = mutableSetOf(),
    @Deprecated(message = "legacy")
    val animatedPackSet: MutableSet<StickerInfo> = mutableSetOf(),
    @Deprecated(message = "legacy")
    val videoPackSet: MutableSet<StickerInfo> = mutableSetOf(),
    val stickerSets: MutableSet<StickerSetEntity> = mutableSetOf(),
    val emojiSets: MutableSet<StickerSetEntity> = mutableSetOf(),
    val language: String = "en",
    var migrated: Boolean = false,
    @Version var version: Int? = null,
) {

    sealed interface StickerSetEntity {
        val packName: String
        val stickerSet: MutableSet<StickerInfo>
        val index: Int
    }

    @TypeAlias("STATIC")
    data class StaticStickerSet(
        override val packName: String,
        override val stickerSet: MutableSet<StickerInfo> = mutableSetOf(),
        override val index: Int = 0
    ) : StickerSetEntity

    @TypeAlias("ANIMATED")
    data class AnimatedStickerSet(
        override val packName: String,
        override val stickerSet: MutableSet<StickerInfo> = mutableSetOf(),
        override val index: Int = 0
    ) : StickerSetEntity

    @TypeAlias("VIDEO")
    data class VideoStickerSet(
        override val packName: String,
        override val stickerSet: MutableSet<StickerInfo> = mutableSetOf(),
        override val index: Int = 0
    ) : StickerSetEntity

    data class StickerInfo(
        val fileId: String,
        val filePath: String
    )
}

fun UserEntity.getPackName(packType: StickerPackType): String =
    when (packType) {
        StickerPackType.NORMAL -> normalPackName
        StickerPackType.ANIMATED -> animatedPackName
        StickerPackType.VIDEO -> videoPackName
    }

fun UserEntity.getPackSet(packType: StickerPackType): MutableSet<UserEntity.StickerInfo> =
    when (packType) {
        StickerPackType.NORMAL -> normalPackSet
        StickerPackType.ANIMATED -> animatedPackSet
        StickerPackType.VIDEO -> videoPackSet
    }

val UserEntity.locale: Locale
    get() = Locale.forLanguageTag(language)
