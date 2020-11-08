package com.vdsirotkin.telegram.mystickersbot.util

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage

private val emojiMap = mapOf(
        "laughing" to "\uD83D\uDE02",
        "crying" to "\uD83D\uDE2D",
        "heart" to "❤️",
        "laughing2" to "\uD83E\uDD23",
        "heart_eyes" to "\uD83D\uDE0D",
        "pleased" to "\uD83D\uDE0C",
        "fire" to "\uD83D\uDD25",
        "thinking" to "\uD83E\uDD14",
        "exhausted" to "\uD83D\uDE2B",
        "eye_roll" to "\uD83D\uDE44",
)

fun SendMessage.addEmojiKeyboard(): SendMessage {
    return emojiMap
            .entries
            .map { InlineKeyboardButton(it.value).callbackData("$chooseEmoji${it.key}") }
            .chunked(5)
            .map { it.toTypedArray() }
            .let { this.replyMarkup(InlineKeyboardMarkup(*it.toTypedArray())) }
}

fun parseEmoji(data: String): String {
    return emojiMap[data.removePrefix(chooseEmoji)] ?: throw IllegalArgumentException("$data doesn't contain known emoji")
}