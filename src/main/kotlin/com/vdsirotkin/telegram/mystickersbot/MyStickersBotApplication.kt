package com.vdsirotkin.telegram.mystickersbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.telegram.telegrambots.ApiContextInitializer

@SpringBootApplication
class MyStickersBotApplication

fun main(args: Array<String>) {
    ApiContextInitializer.init()
    runApplication<MyStickersBotApplication>(*args)
}
