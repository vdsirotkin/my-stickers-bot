package com.vdsirotkin.telegram.mystickersbot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.util.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class StartHandler(
        private val dao: StickerDAO,
        private val botConfigProps: BotConfigProps
) : BaseHandler {

    override fun handle(bot: Bot, update: Update) {
        val normalPackName = "def_stckr_${update.message!!.chat.id}_by_${botConfigProps.username}"
        val animatedPackName = "anim_stckr_${update.message!!.chat.id}_by_${botConfigProps.username}"
        runBlocking (Dispatchers.IO) {
            dao.saveUserPacks(update.message!!.chat.id, normalPackName, animatedPackName)
            bot.sendMessage(update.message!!.chat.id, "Hello! Start sending me stickers, and i'll add them to your personal pack!").body()
        }
    }

}
