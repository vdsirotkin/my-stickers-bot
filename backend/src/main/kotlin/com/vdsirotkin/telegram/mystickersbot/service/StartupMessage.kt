package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.request.SendMessage
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
@Profile("heroku")
class StartupMessage(
        private val bot: MyStickersBot,
        private val props: BotConfigProps
) {

    @EventListener(ContextRefreshedEvent::class)
    fun started() {
        bot.execute(SendMessage(props.serviceAccountId, "I'm alive, version = ${System.getenv("HEROKU_RELEASE_VERSION")}"))
    }

}
