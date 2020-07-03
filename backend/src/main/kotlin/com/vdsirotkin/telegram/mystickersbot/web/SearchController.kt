package com.vdsirotkin.telegram.mystickersbot.web

import com.vdsirotkin.telegram.mystickersbot.bot.MyStickersBot
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat

@Controller
class IndexController(
        private val myStickersBot: MyStickersBot
) {

    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("search", SearchRequest())
        return "search"
    }

    @PostMapping("/search/userId")
    fun search(@ModelAttribute searchRequest: SearchRequest, model: Model) = mono {
        val chatId = searchRequest.userId!!.toLong()
        val chat = myStickersBot.executeAsync(GetChat(chatId))
        model.addAttribute("chat", chat)
        model.addAttribute("search", searchRequest)
        "search"
    }

}

data class SearchRequest(var userId: String?) {
    constructor() : this(null)
}
