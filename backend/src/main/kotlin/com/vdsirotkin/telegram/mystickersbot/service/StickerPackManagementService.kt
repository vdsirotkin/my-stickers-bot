package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.MaskPosition
import com.pengrad.telegrambot.model.Sticker
import com.pengrad.telegrambot.model.request.InputSticker
import com.pengrad.telegrambot.request.AddStickerToSet
import com.pengrad.telegrambot.request.CreateNewStickerSet
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import org.springframework.stereotype.Service

@Service
class StickerPackManagementService(
        private val props: BotConfigProps
) {

    fun static() = Static()
    fun video() = Video()
    fun animated() = Animated()

    inner class Static {
        suspend fun addStickerToPack(bot: TelegramBot,
                                     chatId: Long,
                                     stickerFileId: String,
                                     entity: UserEntity,
                                     emoji: String? = null) {
            bot.executeAsync(AddStickerToSet(chatId, entity.normalPackName, InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))))
        }

        suspend fun createNewPack(bot: TelegramBot,
                                  chatId: Long,
                                  stickerFileId: String,
                                  entity: UserEntity,
                                  emoji: String? = null) {
            bot.executeAsync(CreateNewStickerSet(chatId,
                entity.normalPackName,
                "Your stickers - @${props.username}",
                arrayOf(InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))),
                Sticker.Format.Static,
            ))
        }
    }

    inner class Animated {

        suspend fun addStickerToPack(bot: TelegramBot,
                                     chatId: Long,
                                     stickerFileId: String,
                                     entity: UserEntity,
                                     emoji: String? = null) {
            bot.executeAsync(AddStickerToSet(chatId, entity.animatedPackName, InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))))
        }

        suspend fun createNewPack(bot: TelegramBot,
                                  chatId: Long,
                                  stickerFileId: String,
                                  entity: UserEntity,
                                  emoji: String? = null) {
            bot.executeAsync(CreateNewStickerSet(
                chatId,
                entity.animatedPackName,
                "Your stickers - @${props.username}",
                arrayOf(InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))),
                Sticker.Format.animated
            ))
        }
    }

    inner class Video {

        suspend fun addStickerToPack(bot: TelegramBot,
                                     chatId: Long,
                                     stickerFileId: String,
                                     entity: UserEntity,
                                     emoji: String? = null) {
            bot.executeAsync(AddStickerToSet(chatId, entity.videoPackName, InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))))
        }

        suspend fun createNewPack(bot: TelegramBot,
                                  chatId: Long,
                                  stickerFileId: String,
                                  entity: UserEntity,
                                  emoji: String? = null,
                                  maskPosition: MaskPosition? = null) {
            bot.executeAsync(CreateNewStickerSet(chatId,
                entity.videoPackName,
                "Your stickers - @${props.username}",
                arrayOf(InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))),
                Sticker.Format.video,
            ))
        }
    }
}
