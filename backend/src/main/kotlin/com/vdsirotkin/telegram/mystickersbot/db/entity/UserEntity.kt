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
    val stickerSets: MutableSet<StickerSetEntity> = mutableSetOf(),
    val emojiSets: MutableSet<StickerSetEntity> = mutableSetOf(),
    val language: String = "en",
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

    val normalPackCreated: Boolean
        get() = stickerSets.filterIsInstance<StaticStickerSet>().isNotEmpty()

    val animatedPackCreated: Boolean
        get() = stickerSets.filterIsInstance<AnimatedStickerSet>().isNotEmpty()

    val videoPackCreated: Boolean
        get() = stickerSets.filterIsInstance<VideoStickerSet>().isNotEmpty()
}

fun UserEntity.getPackName(packType: StickerPackType): String =
    when (packType) {
        StickerPackType.NORMAL -> normalPackName
        StickerPackType.ANIMATED -> animatedPackName
        StickerPackType.VIDEO -> videoPackName
    }

fun UserEntity.getPackSets(packType: StickerPackType) =
    when (packType) {
        StickerPackType.NORMAL -> stickerSets.filterIsInstance<UserEntity.StaticStickerSet>()
        StickerPackType.ANIMATED -> stickerSets.filterIsInstance<UserEntity.AnimatedStickerSet>()
        StickerPackType.VIDEO -> stickerSets.filterIsInstance<UserEntity.VideoStickerSet>()
    }

val UserEntity.locale: Locale
    get() = Locale.forLanguageTag(language)
