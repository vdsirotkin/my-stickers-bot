package com.vdsirotkin.telegram.mystickersbot.config

import com.pengrad.telegrambot.TelegramException
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataAccessException
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
                        is TelegramException -> it.response().errorCode() == 429
                        else -> true
                    }
                }
                .maxAttempts(5)
                .build()
        )
    }

    @Bean
    fun dbRetry(): Retry {
        return Retry.of("database", RetryConfig.custom<Any>()
            .retryOnException { it is DataAccessException }
            .build()
        )
    }

    @Bean
    fun rateLimiter(): RateLimiter {
        return RateLimiter.of("telegram_api", RateLimiterConfig.custom()
                .limitForPeriod(25)
                .limitRefreshPeriod(ofSeconds(2))
                .timeoutDuration(Duration.ofHours(3))
                .build())
    }
}
