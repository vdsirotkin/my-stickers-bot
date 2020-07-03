package com.vdsirotkin.telegram.mystickersbot.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document
data class UserEntity(
        @field:Id
        val userId: String,
        val normalPackName: String,
        val animatedPackName: String,
        var normalPackCreated: Boolean = false,
        var animatedPackCreated: Boolean = false,
        val normalPackSet: MutableSet<StickerInfo> = mutableSetOf(),
        val animatedPackSet: MutableSet<StickerInfo> = mutableSetOf(),
        val language: String = "en"
) {
        data class StickerInfo(
                val fileId: String,
                val filePath: String
        )
}

val UserEntity.locale: Locale
        get() = Locale.forLanguageTag(language)
