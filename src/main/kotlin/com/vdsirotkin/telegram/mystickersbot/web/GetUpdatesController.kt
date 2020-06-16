package com.vdsirotkin.telegram.mystickersbot.web

import com.github.kotlintelegrambot.Bot
import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class GetUpdatesController {

    @Bean
    fun routerFunction(props: BotConfigProps, bot: Bot): RouterFunction<ServerResponse> {
        return coRouter {
            POST("/${props.token}") { request ->
                request.body(BodyExtractors.toMono(String::class.java))
                        .doOnNext { bot.processUpdate(it) }
                        .flatMap { ok().build() }
                        .awaitLast()
            }
        }
    }

}
