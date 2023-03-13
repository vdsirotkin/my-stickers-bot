package com.vdsirotkin.telegram.mystickersbot.handler.common

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.db.StickerDAO
import com.vdsirotkin.telegram.mystickersbot.dto.SendMessageWithAction
import com.vdsirotkin.telegram.mystickersbot.dto.StickerPackType
import com.vdsirotkin.telegram.mystickersbot.handler.BaseHandler
import com.vdsirotkin.telegram.mystickersbot.util.PackNameProvider
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.sokomishalov.commons.core.log.Loggable
import java.util.*

@Service
class StartHandler(
    private val dao: StickerDAO,
    private val messageSource: MessageSource,
    private val packNameProvider: PackNameProvider,
) : BaseHandler {
    override fun handle(bot: TelegramBot, update: Update): Mono<BaseHandler> {
        val chatId = update.message().chat().id()
        val normalPackName = with(packNameProvider) { StickerPackType.NORMAL(chatId, 0) }
        val animatedPackName = with(packNameProvider) { StickerPackType.ANIMATED(chatId, 0) }
        val vidPackName = with(packNameProvider) { StickerPackType.VIDEO(chatId, 0) }
        return mdcMono {
            if (!dao.userRegistered(chatId)) {
                dao.saveUserPacks(chatId, normalPackName, animatedPackName, vidPackName)
            }
            bot.executeAsync(SendMessageWithAction(chatId, messageSource.getMessage("welcome", null, Locale.ENGLISH), action))
        }.thenReturn(this)
    }

    override val action: String
        get() = "NEW_USER_OR_HELP"

    companion object : Loggable

}
