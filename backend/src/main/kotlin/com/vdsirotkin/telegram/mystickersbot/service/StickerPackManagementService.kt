package com.vdsirotkin.telegram.mystickersbot.service

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.entity.UserEntity
import com.vdsirotkin.telegram.mystickersbot.util.executeStickerPackAction
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.stickers.AddStickerToSet
import org.telegram.telegrambots.meta.api.methods.stickers.CreateNewStickerSet
import org.telegram.telegrambots.meta.api.objects.stickers.MaskPosition
import java.io.File

@Service
class StickerPackManagementService(
        private val props: BotConfigProps
) {

    suspend fun addStickerToPack(bot: DefaultAbsSender,
                                         chatId: Long,
                                         sticker: File,
                                         entity: UserEntity,
                                         emoji: String? = null) {
        bot.executeStickerPackAction(AddStickerToSet(chatId.toInt(), entity.normalPackName, emoji ?: "🙂").setPngSticker(sticker))
    }

    suspend fun createNewPack(bot: DefaultAbsSender,
                                      chatId: Long,
                                      sticker: File,
                                      entity: UserEntity,
                                      emoji: String? = null,
                                      maskPosition: MaskPosition? = null) {
        bot.executeStickerPackAction(CreateNewStickerSet(chatId.toInt(), entity.normalPackName, "Your stickers - @${props.username}", emoji ?: "🙂").setPngStickerFile(sticker)
                .apply {
                    this.containsMasks = maskPosition != null
                    this.maskPosition = maskPosition
                })
    }

}
