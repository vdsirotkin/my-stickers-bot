package com.vdsirotkin.telegram.mystickersbot.dto

import com.pengrad.telegrambot.model.Sticker

enum class StickerPackType {
    NORMAL, ANIMATED, VIDEO
}

fun Sticker.packType() =
    if (this.isVideo)
        StickerPackType.VIDEO
    else if (this.isAnimated)
        StickerPackType.ANIMATED
    else
        StickerPackType.NORMAL

