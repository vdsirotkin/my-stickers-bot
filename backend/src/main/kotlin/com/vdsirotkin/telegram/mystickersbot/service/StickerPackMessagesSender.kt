package com.vdsirotkin.telegram.mystickersbot.service

import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.addInlineKeyboard
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.packLink
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Service
class StickerPackMessagesSender {

    suspend fun sendSuccessAdd(bot: DefaultAbsSender,
                               chatId: Long,
                               messageSource: MessageSourceWrapper,
                               messageId: Int,
                               entity: UserEntity) {
        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("sticker.added"))
                .setReplyToMessageId(messageId)
                .addInlineKeyboard(messageSource.getMessage("sticker.pack.button.text"), packLink(entity.normalPackName))
        )
    }

    suspend fun sendSuccessCreated(bot: DefaultAbsSender, chatId: Long,
                                   messageSource: MessageSourceWrapper,
                                   messageId: Int,
                                   entity: UserEntity) {
        bot.executeAsync(SendMessage(chatId, messageSource.getMessage("created.pack"))
                .setReplyToMessageId(messageId)
                .addInlineKeyboard(messageSource.getMessage("sticker.pack.button.text"), packLink(entity.normalPackName)))
    }

}
