package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Sticker
import com.pengrad.telegrambot.model.request.InputSticker
import com.pengrad.telegrambot.request.AddStickerToSet
import com.pengrad.telegrambot.request.CreateNewStickerSet
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import org.springframework.stereotype.Service

@Service
class EmojiPackManagementService(
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
            bot.executeAsync(AddStickerToSet(chatId, entity.staticEmojiSetName, InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))))
        }

        suspend fun createNewPack(
            bot: TelegramBot,
            chatId: Long,
            packName: String,
            stickerFileId: String,
            emoji: String? = null,
        ) {
            bot.executeAsync(CreateNewStickerSet(chatId,
                packName,
                "Your emojis - @${props.username}",
                arrayOf(InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))),
                Sticker.Format.Static,
            ).stickerType(Sticker.Type.custom_emoji))
        }
    }

    inner class Animated {

        suspend fun addStickerToPack(bot: TelegramBot,
                                     chatId: Long,
                                     stickerFileId: String,
                                     entity: UserEntity,
                                     emoji: String? = null) {
            bot.executeAsync(AddStickerToSet(chatId, entity.animatedEmojiSetName, InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))))
        }

        suspend fun createNewPack(
            bot: TelegramBot,
            chatId: Long,
            packName: String,
            stickerFileId: String,
            emoji: String? = null,
        ) {
            bot.executeAsync(CreateNewStickerSet(
                chatId,
                packName,
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
            bot.executeAsync(AddStickerToSet(chatId, entity.videoEmojiSetName, InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))))
        }

        suspend fun createNewPack(
            bot: TelegramBot,
            chatId: Long,
            packName: String,
            stickerFileId: String,
            emoji: String? = null,
        ) {
            bot.executeAsync(CreateNewStickerSet(chatId,
                packName,
                "Your stickers - @${props.username}",
                arrayOf(InputSticker(stickerFileId, arrayOf(emoji ?: "🙂"))),
                Sticker.Format.video,
            ))
        }
    }
}
