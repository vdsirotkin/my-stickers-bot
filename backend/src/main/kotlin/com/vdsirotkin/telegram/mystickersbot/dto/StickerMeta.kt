package com.vdsirotkin.telegram.mystickersbot.dto

data class StickerMeta(
    val fileId: String,
    val fileUniqueId: String,
    val emoji: String? = null,
    val type: StickerPackType,
    val createdStickerFileId: String? = null
)
