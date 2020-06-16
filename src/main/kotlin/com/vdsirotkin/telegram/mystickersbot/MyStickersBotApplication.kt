package com.vdsirotkin.telegram.mystickersbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MyStickersBotApplication

fun main(args: Array<String>) {
    runApplication<MyStickersBotApplication>(*args)
}
