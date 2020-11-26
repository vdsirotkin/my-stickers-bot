package com.vdsirotkin.telegram.mystickersbot.dto

import com.pengrad.telegrambot.request.SendMessage

class SendMessageWithAction(chatId: Any, text: String, val action: String) : SendMessage(chatId, text) {

    override fun getMethod(): String {
        return "sendMessage"
    }
}