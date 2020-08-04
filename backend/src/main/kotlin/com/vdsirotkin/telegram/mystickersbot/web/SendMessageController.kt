package com.vdsirotkin.telegram.mystickersbot.web

import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.sokomishalov.commons.core.log.Loggable

@Controller
class SendMessageController(
        private val bot: MyStickersBot
) {

    @GetMapping("/send")
    fun sendIndex(model: Model): String {
        model.addAttribute("sendMessage", SendMessageRequest())
        return "send-message"
    }

    @GetMapping("/send/all")
    fun sendAllIndex(model: Model): String {
        model.addAttribute("sendMessage", SendMessageRequest())
        return "send-message"
    }

    @PostMapping("/send")
    suspend fun send(@ModelAttribute sendMessageRequest: SendMessageRequest, model: Model): String {
        val result = try {
            bot.executeAsync(SendMessage(sendMessageRequest.userId.toLong(), sendMessageRequest.text).enableMarkdownV2(true))
            "Success"
        } catch (e: Exception) {
            logError(e.message, e)
            "Error"
        }

        model.addAttribute("sendMessage", sendMessageRequest)
        model.addAttribute("result", result)
        return "send-message"
    }

    data class SendMessageRequest(var userId: String, var text: String) {
        constructor() : this("", "")
    }

    companion object : Loggable

}
