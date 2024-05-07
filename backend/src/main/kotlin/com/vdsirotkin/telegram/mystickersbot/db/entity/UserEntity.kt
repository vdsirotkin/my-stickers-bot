package com.vdsirotkin.telegram.mystickersbot.db.entity

import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
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
    var videoPackName: String = "",
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

    @TypeAlias("STATIC_EMOJI")
    data class StaticEmojiSet(
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

    @TypeAlias("ANIMATED_EMOJI")
    data class AnimatedEmojiSet(
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

    @TypeAlias("VIDEO_EMOJI")
    data class VideoEmojiSet(
        override val packName: String,
        override val stickerSet: MutableSet<StickerInfo> = mutableSetOf(),
        override val index: Int = 0
    ) : StickerSetEntity

    data class StickerInfo(
        @Field("fileId")
        val originalStickerFileUniqueId: String,
        val createdStickerFileUniqueId: String? = null
    )

    val normalPackCreated: Boolean
        get() = stickerSets.filterIsInstance<StaticStickerSet>().isNotEmpty()

    val animatedPackCreated: Boolean
        get() = stickerSets.filterIsInstance<AnimatedStickerSet>().isNotEmpty()

    val videoPackCreated: Boolean
        get() = stickerSets.filterIsInstance<VideoStickerSet>().isNotEmpty()

    val staticEmojiPackCreated: Boolean
        get() = emojiSets.filterIsInstance<StaticEmojiSet>().isNotEmpty()

    val staticEmojiSetName: String
        get() = emojiSets.filterIsInstance<StaticEmojiSet>().firstOrNull()?.packName ?: ""

    val animatedEmojiPackCreated: Boolean
        get() = emojiSets.filterIsInstance<AnimatedEmojiSet>().isNotEmpty()

    val animatedEmojiSetName: String
        get() = emojiSets.filterIsInstance<AnimatedEmojiSet>().firstOrNull()?.packName ?: ""

    val videoEmojiPackCreated: Boolean
        get() = emojiSets.filterIsInstance<VideoEmojiSet>().isNotEmpty()

    val videoEmojiSetName: String
        get() = emojiSets.filterIsInstance<VideoEmojiSet>().firstOrNull()?.packName ?: ""
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
