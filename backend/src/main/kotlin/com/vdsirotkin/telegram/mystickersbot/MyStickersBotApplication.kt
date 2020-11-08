package com.vdsirotkin.telegram.mystickersbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@EnableScheduling
class MyStickersBotApplication

fun main(args: Array<String>) {
    runApplication<MyStickersBotApplication>(*args)
}
