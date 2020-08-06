package com.vdsirotkin.telegram.mystickersbot.service

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Service
class StartupMessage(
        private val bot: MyStickersBot,
        private val props: BotConfigProps
) {

    @Profile("heroku")
    @EventListener(ContextRefreshedEvent::class)
    fun started() {
        bot.execute(SendMessage(props.serviceAccountId, "I'm alive, version = ${System.getenv("HEROKU_RELEASE_VERSION")}"))
    }

}
