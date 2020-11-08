package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.MaskPosition
import com.pengrad.telegrambot.request.AddStickerToSet
import com.pengrad.telegrambot.request.CreateNewStickerSet
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.executeStickerPackAction
import org.springframework.stereotype.Service
import java.io.File

@Service
class StickerPackManagementService(
        private val props: BotConfigProps
) {

    suspend fun addStickerToPack(bot: TelegramBot,
                                         chatId: Long,
                                         sticker: File,
                                         entity: UserEntity,
                                         emoji: String? = null) {
        bot.executeStickerPackAction(AddStickerToSet(chatId.toInt(), entity.normalPackName, sticker, emoji ?: "🙂"))
    }

    suspend fun createNewPack(bot: TelegramBot,
                                      chatId: Long,
                                      sticker: File,
                                      entity: UserEntity,
                                      emoji: String? = null,
                                      maskPosition: MaskPosition? = null) {
        bot.executeStickerPackAction(CreateNewStickerSet(chatId.toInt(), entity.normalPackName, "Your stickers - @${props.username}", sticker, emoji ?: "🙂")
                .containsMasks(maskPosition != null)
                .maskPosition(maskPosition))
    }

}
