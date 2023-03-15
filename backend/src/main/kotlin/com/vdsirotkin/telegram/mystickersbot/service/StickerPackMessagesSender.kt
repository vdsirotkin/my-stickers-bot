package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.TelegramBot
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.addInlineKeyboard
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.packLink
import org.springframework.stereotype.Service

@Service
class StickerPackMessagesSender {

    fun static() = Static()
    fun animated() = Animated()
    fun video() = Video()

    inner class Static {
        suspend fun sendSuccessAdd(bot: TelegramBot,
                                   chatId: Long,
                                   messageSource: MessageSourceWrapper,
                                   messageId: Int,
                                   entity: UserEntity, action: String) {
            bot.executeAsync(SendMessageWithAction(chatId, messageSource["sticker.added"], action)
                .replyToMessageId(messageId)
                .addInlineKeyboard(messageSource["sticker.pack.button.text"], packLink(entity.normalPackName))
            )
        }

        suspend fun sendSuccessCreated(bot: TelegramBot, chatId: Long,
                                       messageSource: MessageSourceWrapper,
                                       messageId: Int,
                                       entity: UserEntity, action: String) {
            bot.executeAsync(SendMessageWithAction(chatId, messageSource["created.pack"], action)
                .replyToMessageId(messageId)
                .addInlineKeyboard(messageSource["sticker.pack.button.text"], packLink(entity.normalPackName)))
        }
    }

    inner class Animated {
        suspend fun sendSuccessAdd(bot: TelegramBot, chatId: Long, messageId: Int, messageSource: MessageSourceWrapper, userEntity: UserEntity, action: String) {
            bot.executeAsync(
                SendMessageWithAction(chatId, messageSource["sticker.added"], action)
                    .replyToMessageId(messageId)
                    .addInlineKeyboard(messageSource["animated.sticker.pack.button.text"], packLink(userEntity.animatedPackName))
            )
        }

        suspend fun sendSuccessCreated(
            bot: TelegramBot,
            chatId: Long,
            messageId: Int,
            messageSource: MessageSourceWrapper,
            userEntity: UserEntity, action: String
        ) {
            bot.executeAsync(
                SendMessageWithAction(chatId, messageSource["created.pack"], action)
                    .replyToMessageId(messageId)
                    .addInlineKeyboard(messageSource["animated.sticker.pack.button.text"], packLink(userEntity.animatedPackName))
            )
        }
    }

    inner class Video {
        suspend fun sendSuccessAdd(bot: TelegramBot,
                                   chatId: Long,
                                   messageSource: MessageSourceWrapper,
                                   messageId: Int,
                                   entity: UserEntity, action: String) {
            bot.executeAsync(SendMessageWithAction(chatId, messageSource["sticker.added"], action)
                .replyToMessageId(messageId)
                .addInlineKeyboard(messageSource["sticker.pack.button.text"], packLink(entity.videoPackName))
            )
        }

        suspend fun sendSuccessCreated(bot: TelegramBot, chatId: Long,
                                       messageSource: MessageSourceWrapper,
                                       messageId: Int,
                                       entity: UserEntity, action: String) {
            bot.executeAsync(SendMessageWithAction(chatId, messageSource["created.pack"], action)
                .replyToMessageId(messageId)
                .addInlineKeyboard(messageSource["sticker.pack.button.text"], packLink(entity.videoPackName)))
        }
    }

}
