package com.vdsirotkin.telegram.mystickersbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.telegram.telegrambots.ApiContextInitializer

@SpringBootApplication
@EnableScheduling
class MyStickersBotApplication

fun main(args: Array<String>) {
    ApiContextInitializer.init()
    runApplication<MyStickersBotApplication>(*args)
}
