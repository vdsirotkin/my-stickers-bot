package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.dao.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.util.monoWithMdc
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable

@Service
class StartHandler(
        private val dao: StickerDAO,
        private val botConfigProps: BotConfigProps
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<Unit> {
        val normalPackName = "def_stckr_${update.message!!.chat.id}_by_${botConfigProps.username}"
        val animatedPackName = "anim_stckr_${update.message!!.chat.id}_by_${botConfigProps.username}"
        return monoWithMdc {
            logger.info("New user joined")
            dao.saveUserPacks(update.message!!.chat.id, normalPackName, animatedPackName)
            bot.execute(SendMessage(update.message!!.chat.id, "Hello! Start sending me stickers, and i'll add them to your personal pack!"))
            Unit
        }
    }

    companion object : Loggable

}