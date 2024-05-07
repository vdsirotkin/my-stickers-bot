package com.vdsirotkin.telegram.mystickersbot.util

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import org.springframework.stereotype.Component

@Component
class PackNameProvider(
    private val props: BotConfigProps,
) {

    operator fun StickerPackType.invoke(chatId: Long, index: Int): String {
        return when (this) {
            StickerPackType.NORMAL -> if (index == 0) {
                "def_stckr_${chatId}_by_${props.username}"
            } else "def_stckr_${chatId}_${index}_by_${props.username}"

            StickerPackType.ANIMATED -> if (index == 0) {
                "anim_stckr_${chatId}_by_${props.username}"
            } else "anim_stckr_${chatId}_${index}_by_${props.username}"

            StickerPackType.VIDEO -> if (index == 0) {
                "vid_stckr_${chatId}_by_${props.username}"
            } else "vid_stckr_${chatId}_${index}_by_${props.username}"
        }
    }

    fun generateEmojiPackName(packType: StickerPackType, chatId: Long, index: Int = 0): String {
        return when (packType) {
            StickerPackType.NORMAL -> if (index == 0) {
                "def_emoji_${chatId}_by_${props.username}"
            } else "def_emoji_${chatId}_${index}_by_${props.username}"

            StickerPackType.ANIMATED -> if (index == 0) {
                "anim_emoji_${chatId}_by_${props.username}"
            } else "anim_emoji_${chatId}_${index}_by_${props.username}"

            StickerPackType.VIDEO -> if (index == 0) {
                "vid_emoji_${chatId}_by_${props.username}"
            } else "vid_emoji_${chatId}_${index}_by_${props.username}"
        }
    }
}
