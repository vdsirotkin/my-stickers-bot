package com.vdsirotkin.telegram.mystickersbot

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.telegram.telegrambots.ApiContextInitializer

@SpringBootApplication
@EnableConfigurationProperties(BotConfigProps::class)
class MyStickersBotApplication

fun main(args: Array<String>) {
    ApiContextInitializer.init()
    runApplication<MyStickersBotApplication>(*args)
}
