package com.vdsirotkin.telegram.mystickersbot.util

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class HerokuActuator : InfoContributor {

    val metadataEnvVars = mapOf(
            "HEROKU_APP_ID" to "appId",
            "HEROKU_APP_NAME" to "appName",
            "HEROKU_DYNO_ID" to "dynoId",
            "HEROKU_RELEASE_CREATED_AT" to "createdAt",
            "HEROKU_RELEASE_VERSION" to "version",
            "HEROKU_SLUG_COMMIT" to "commit",
            "HEROKU_SLUG_DESCRIPTION" to "description"
    )

    val metadataCache = mutableMapOf<String, String>()

    @PostConstruct
    fun init() {
        metadataEnvVars.keys.forEach { key ->
            System.getenv(key)?.let {
                metadataCache[metadataEnvVars[key]!!] = it
            }
        }
    }

    override fun contribute(builder: Info.Builder) {
        builder.withDetail("heroku", metadataCache)
    }
}
