package com.vdsirotkin.telegram.mystickersbot.handler.emojis

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Sticker
import com.pengrad.telegrambot.model.Update
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.packType
import com.vdsirotkin.telegram.mystickersbot.service.EmojiPackManagementService
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.PackNameProvider
import com.vdsirotkin.telegram.mystickersbot.util.determineChatId
import org.springframework.stereotype.Component

@Component
class EmojiProcessor(
    private val stickerDAO: StickerDAO,
    private val packNameProvider: PackNameProvider,
    private val emojiPackManagementService: EmojiPackManagementService,
) {
    suspend fun handleStaticEmoji(
        bot: TelegramBot,
        update: Update,
        messageSource: MessageSourceWrapper,
        userEntity: UserEntity,
        customEmoji: Sticker,
    ) {
        val packType = customEmoji.packType()
        val chatId = determineChatId(update)
        if (userEntity.staticEmojiPackCreated) {
            emojiPackManagementService.static().addStickerToPack(bot, chatId, customEmoji.fileId(), userEntity, customEmoji.emoji())
        } else {
            val packName = packNameProvider.generateEmojiPackName(packType, chatId)
            emojiPackManagementService.static().createNewPack(bot, chatId, packName, customEmoji.fileId(), customEmoji.emoji())
            stickerDAO.createEmojiSet(chatId, packType, packName)
        }
    }

    suspend fun handleAnimatedEmoji(
        bot: TelegramBot,
        update: Update,
        messageSource: MessageSourceWrapper,
        userEntity: UserEntity,
        customEmoji: Sticker,
    ) {
        val packType = customEmoji.packType()
        val chatId = determineChatId(update)
        if (userEntity.animatedEmojiPackCreated) {
            emojiPackManagementService.animated().addStickerToPack(bot, chatId, customEmoji.fileId(), userEntity, customEmoji.emoji())
        } else {
            val packName = packNameProvider.generateEmojiPackName(packType, chatId)
            emojiPackManagementService.animated().createNewPack(bot, chatId, packName, customEmoji.fileId(), customEmoji.emoji())
            stickerDAO.createEmojiSet(chatId, packType, packName)
        }
    }

    suspend fun handleVideoEmoji(
        bot: TelegramBot,
        update: Update,
        messageSource: MessageSourceWrapper,
        userEntity: UserEntity,
        customEmoji: Sticker,
    ) {
        val packType = customEmoji.packType()
        val chatId = determineChatId(update)
        if (userEntity.videoEmojiPackCreated) {
            emojiPackManagementService.video().addStickerToPack(bot, chatId, customEmoji.fileId(), userEntity, customEmoji.emoji())
        } else {
            val packName = packNameProvider.generateEmojiPackName(packType, chatId)
            emojiPackManagementService.video().createNewPack(bot, chatId, packName, customEmoji.fileId(), customEmoji.emoji())
            stickerDAO.createEmojiSet(chatId, packType, packName)
        }
    }
}
