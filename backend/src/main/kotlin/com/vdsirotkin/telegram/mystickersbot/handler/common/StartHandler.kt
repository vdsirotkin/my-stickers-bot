package com.vdsirotkin.telegram.mystickersbot.handler.common

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable
import java.util.*

@Service
class StartHandler(
        private val dao: StickerDAO,
        private val messageSource: MessageSource,
        private val botConfigProps: BotConfigProps
) : BaseHandler {

    override fun handle(bot: DefaultAbsSender, update: Update): Mono<BaseHandler> {
        val chatId = update.message!!.chat.id
        val normalPackName = "def_stckr_${chatId}_by_${botConfigProps.username}"
        val animatedPackName = "anim_stckr_${chatId}_by_${botConfigProps.username}"
        return mdcMono {
            if (!dao.userRegistered(chatId)) {
                dao.saveUserPacks(chatId, normalPackName, animatedPackName)
                logger.info("New user joined")
            } else {
                logger.warn("User already registered")
            }
            bot.executeAsync(SendMessage(chatId, messageSource.getMessage("welcome", null, Locale.ENGLISH)))
        }.thenReturn(this)
    }

    companion object : Loggable

}
