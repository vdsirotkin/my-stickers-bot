package com.vdsirotkin.telegram.mystickersbot.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.webhook
import com.vdsirotkin.telegram.mystickersbot.handler.AnimatedStickerHandler
import com.vdsirotkin.telegram.mystickersbot.handler.NormalStickerHandler
import com.vdsirotkin.telegram.mystickersbot.handler.StartHandler
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(BotConfigProps::class)
class BotConfiguration {

    @Bean
    fun bot(
            props: BotConfigProps,
            startHandler: StartHandler,
            normalStickerHandler: NormalStickerHandler,
            animatedStickerHandler: AnimatedStickerHandler
    ): Bot {
        return bot {
            token = props.token
            if (props.webhook) {
                webhook {
                    url = props.webhookUrl
                }
            }
            dispatch {
                command("start", startHandler::handle)
                message(Filter.Sticker) { bot, update ->
                    if (update.message!!.sticker!!.isAnimated) {
                        animatedStickerHandler.handle(bot, update)
                    } else {
                        normalStickerHandler.handle(bot, update)
                    }
                }
                message(Filter.Text) { bot, update ->
                    bot.sendMessage(update.message!!.chat.id, "Unknown command, please check /help")
                }
            }
        }.also {
            if (props.webhook) {
                it.startWebhook()
            } else {
                it.startPolling()
            }
        }
    }

}
