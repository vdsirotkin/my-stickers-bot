package com.vdsirotkin.telegram.mystickersbot.bot

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "bot")
data class BotConfigProps(
        val webhook: Boolean,
        val token: String,
        val username: String,
        val webhookUrl: String?,
        val serviceAccountId: Long
)
