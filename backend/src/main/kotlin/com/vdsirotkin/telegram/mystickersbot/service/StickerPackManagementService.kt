package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.MaskPosition
import com.pengrad.telegrambot.model.Sticker
import com.pengrad.telegrambot.request.AddStickerToSet
import com.pengrad.telegrambot.request.CreateNewStickerSet
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import org.springframework.stereotype.Service
import java.io.File

@Service
class StickerPackManagementService(
        private val props: BotConfigProps
) {

    fun video() = Video()
    fun static() = Static()
    fun animated() = Animated()

    inner class Animated {
        suspend fun addStickerToPack(
            bot: TelegramBot,
            chatId: Long,
            userEntity: UserEntity,
            sticker: Sticker,
            stickerFile: File
        ) {
            bot.executeAsync(AddStickerToSet.tgsSticker(chatId, userEntity.animatedPackName, sticker.emoji() ?: "🙂", stickerFile)
                .apply {
                    if (sticker.maskPosition() != null) {
                        maskPosition(sticker.maskPosition())
                    }
                })
        }

        suspend fun createNewPack(
            bot: TelegramBot,
            chatId: Long?,
            userEntity: UserEntity,
            sticker: Sticker,
            stickerFile: File
        ) {
            bot.executeAsync(CreateNewStickerSet.tgsSticker(chatId, userEntity.animatedPackName, "Your animated stickers - @${props.username}", sticker.emoji() ?: "🙂", stickerFile)
                .apply {
                    if (sticker.maskPosition() != null) {
                        maskPosition(sticker.maskPosition())
                    }
                }
            )
        }
    }

    inner class Static {
        suspend fun addStickerToPack(bot: TelegramBot,
                                     chatId: Long,
                                     sticker: File,
                                     entity: UserEntity,
                                     emoji: String? = null) {
            bot.executeAsync(AddStickerToSet.pngSticker(chatId, entity.normalPackName, emoji ?: "🙂", sticker))
        }

        suspend fun createNewPack(bot: TelegramBot,
                                  chatId: Long,
                                  sticker: File,
                                  entity: UserEntity,
                                  emoji: String? = null,
                                  maskPosition: MaskPosition? = null) {
            bot.executeAsync(CreateNewStickerSet.pngSticker(chatId, entity.normalPackName, "Your stickers - @${props.username}", emoji ?: "🙂", sticker)
                .apply {
                    if (maskPosition != null) {
                        maskPosition(maskPosition)
                    }
                })
        }
    }

    inner class Video {
        suspend fun addStickerToPack(bot: TelegramBot,
                                     chatId: Long,
                                     sticker: File,
                                     entity: UserEntity,
                                     emoji: String? = null) {
            bot.executeAsync(AddStickerToSet.webmSticker(chatId, entity.videoPackName, emoji ?: "🙂", sticker))
        }

        suspend fun createNewPack(bot: TelegramBot,
                                  chatId: Long,
                                  sticker: File,
                                  entity: UserEntity,
                                  emoji: String? = null,
                                  maskPosition: MaskPosition? = null) {
            bot.executeAsync(CreateNewStickerSet.webmSticker(chatId, entity.videoPackName, "Your stickers - @${props.username}", emoji ?: "🙂", sticker)
                .apply {
                    if (maskPosition != null) {
                        maskPosition(maskPosition)
                    }
                })
        }
    }

}
