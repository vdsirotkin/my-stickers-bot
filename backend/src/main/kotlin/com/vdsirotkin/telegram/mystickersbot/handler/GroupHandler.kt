package com.vdsirotkin.telegram.mystickersbot.handler

import com.vdsirotkin.telegram.mystickersbot.util.GROUP_MESSAGE
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import com.vdsirotkin.telegram.mystickersbot.util.mdcMono
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.LongPollingBot
import reactor.core.publisher.Mono

class GroupHandler(
        private val chatId: Long,
) : BaseHandler {
    override fun handle(bot: DefaultAbsSender, update: Update): Mono<BaseHandler> = mdcMono {
        val members = update.message!!.newChatMembers!!
        if (members.any { it.userName == (bot as LongPollingBot).botUsername }) {
            bot.executeAsync(SendMessage(chatId, GROUP_MESSAGE))
        }
    }.thenReturn(this)
}