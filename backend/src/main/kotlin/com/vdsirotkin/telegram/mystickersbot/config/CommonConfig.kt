package com.vdsirotkin.telegram.mystickersbot.config

import com.vdsirotkin.telegram.mystickersbot.bot.BotConfigProps
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(BotConfigProps::class)
class CommonConfig
