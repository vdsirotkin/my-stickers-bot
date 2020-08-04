package com.vdsirotkin.telegram.mystickersbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.bots.DefaultBotOptions

@Configuration
class BotOptions {

    @Bean
    fun defaultBotOptions(): DefaultBotOptions {
        return DefaultBotOptions().apply {
            maxThreads = Runtime.getRuntime().availableProcessors() * 2
        }
    }

}
