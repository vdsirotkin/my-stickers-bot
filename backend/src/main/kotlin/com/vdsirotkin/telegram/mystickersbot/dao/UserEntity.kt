package com.vdsirotkin.telegram.mystickersbot.dao

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class UserEntity(
        @field:Id
        val userId: String,
        val normalPackName: String,
        val animatedPackName: String,
        var normalPackCreated: Boolean = false,
        var animatedPackCreated: Boolean = false,
        val normalPackSet: MutableSet<StickerInfo> = mutableSetOf(),
        val animatedPackSet: MutableSet<StickerInfo> = mutableSetOf()
) {
        data class StickerInfo(
                val fileId: String,
                val filePath: String
        )
}
