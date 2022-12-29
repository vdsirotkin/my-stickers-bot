package com.vdsirotkin.telegram.mystickersbot.db.entity

import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

const val USER_ENTITY_COLLECTION_NAME = "userEntity"

@Document(collection = USER_ENTITY_COLLECTION_NAME)
data class UserEntity(
    @field:Id
    val userId: String,
    val normalPackName: String,
    val animatedPackName: String,
    val videoPackName: String = "",
    var normalPackCreated: Boolean = false,
    var animatedPackCreated: Boolean = false,
    var videoPackCreated: Boolean = false,
    val normalPackSet: MutableSet<StickerInfo> = mutableSetOf(),
    val animatedPackSet: MutableSet<StickerInfo> = mutableSetOf(),
    val videoPackSet: MutableSet<StickerInfo> = mutableSetOf(),
    val language: String = "en"
) {
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
