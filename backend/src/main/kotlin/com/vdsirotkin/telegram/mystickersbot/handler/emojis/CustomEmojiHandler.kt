package com.vdsirotkin.telegram.mystickersbot.handler.emojis

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ReplyParameters
import com.pengrad.telegrambot.request.GetCustomEmojiStickers
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import com.vdsirotkin.telegram.mystickersbot.dto.packType
import com.vdsirotkin.telegram.mystickersbot.handler.LocalizedHandler
import com.vdsirotkin.telegram.mystickersbot.util.MessageSourceWrapper
import com.vdsirotkin.telegram.mystickersbot.util.determineChatId
import com.vdsirotkin.telegram.mystickersbot.util.emojiPackLink
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import ru.sokomishalov.commons.core.log.Loggable

@Component
class CustomEmojiHandler(
    override val stickerDao: StickerDAO,
    override val messageSource: MessageSource,
    private val staticEmojiHandler: EmojiProcessor,
) : LocalizedHandler {
    override suspend fun handleInternal(
        bot: TelegramBot,
        update: Update,
        messageSource: MessageSourceWrapper,
        userEntity: UserEntity
    ) {
        val customEmojiIds = update.message().entities().mapNotNull { it.customEmojiId() }
        val emojisResponse = bot.executeAsync(GetCustomEmojiStickers(*customEmojiIds.toTypedArray()))
        val packsInvolved = emojisResponse.result().onEach {
            when (it.packType()) {
                StickerPackType.NORMAL -> staticEmojiHandler.handleStaticEmoji(bot, update, messageSource, userEntity, it)
                StickerPackType.ANIMATED -> staticEmojiHandler.handleAnimatedEmoji(bot, update, messageSource, userEntity, it)
                StickerPackType.VIDEO -> staticEmojiHandler.handleVideoEmoji(bot, update, messageSource, userEntity, it)
            }
        }.map { it.packType() }.distinct()
        if (packsInvolved.isEmpty()) return

        val updatedUser = stickerDao.getUserEntity(userEntity.userId.toLong())
        val text = if (packsInvolved.size > 1) messageSource["emoji.success.plural"] else messageSource["emoji.success"]
        val buttons = packsInvolved.map {
            when (it) {
                StickerPackType.NORMAL -> messageSource["emoji.static.button.text"] to emojiPackLink(updatedUser.staticEmojiSetName)
                StickerPackType.ANIMATED -> messageSource["emoji.animated.button.text"] to emojiPackLink(updatedUser.animatedEmojiSetName)
                StickerPackType.VIDEO -> messageSource["emoji.video.button.text"] to emojiPackLink(updatedUser.videoEmojiSetName)
            }
        }.map { arrayOf(InlineKeyboardButton(it.first).url(it.second)) }

        bot.executeAsync(SendMessageWithAction(determineChatId(update), text, action)
            .replyParameters(ReplyParameters(update.message().messageId()))
            .replyMarkup(InlineKeyboardMarkup(*buttons.toTypedArray()))
        )
    }

    override val action: String
        get() = "CUSTOM_EMOJI"

    companion object : Loggable
}
