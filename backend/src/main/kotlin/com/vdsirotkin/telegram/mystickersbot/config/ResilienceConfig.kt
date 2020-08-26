package com.vdsirotkin.telegram.mystickersbot.config

import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.time.Duration
import java.time.Duration.ofSeconds

@Configuration
class ResilienceConfig {

    @Bean
    fun telegramApiRetry(): Retry {
        return Retry.of("telegram_api", RetryConfig.custom<Any>()
                .intervalFunction(IntervalFunction.ofExponentialBackoff(ofSeconds(1).toMillis(), 2.0))
                .retryOnException {
                    when (it) {
                        is TelegramApiRequestException -> it.errorCode == 429
                        else -> true
                    }
                }
                .maxAttempts(5)
                .build()
        )
    }

    @Bean
    fun rateLimiter(): RateLimiter {
        return RateLimiter.of("telegram_api", RateLimiterConfig.custom()
                .limitForPeriod(27)
                .limitRefreshPeriod(ofSeconds(1))
                .timeoutDuration(Duration.ofHours(3))
                .build())
    }

}
