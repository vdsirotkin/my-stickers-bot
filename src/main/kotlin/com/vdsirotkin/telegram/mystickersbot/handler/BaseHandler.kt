package com.vdsirotkin.telegram.mystickersbot.handler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update

interface BaseHandler {

    fun handle(bot: Bot, update: Update)

}
