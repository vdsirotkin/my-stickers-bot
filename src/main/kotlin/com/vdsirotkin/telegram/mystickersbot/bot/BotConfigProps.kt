package com.vdsirotkin.telegram.mystickersbot.bot

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bot")
@ConstructorBinding
data class BotConfigProps(
        val webhook: Boolean,
        val token: String,
        val username: String,
        val webhookUrl: String?
)
