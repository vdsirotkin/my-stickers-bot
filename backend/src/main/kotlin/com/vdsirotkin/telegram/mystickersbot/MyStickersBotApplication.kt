package com.vdsirotkin.telegram.mystickersbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@EnableScheduling
class MyStickersBotApplication

private fun ensureExternalTools() {
    val runtime = Runtime.getRuntime()
    runtime.exec("ffmpeg -version").also { it.waitFor() }.exitValue().let {
        if (it != 0) {
            throw IllegalStateException("FFmpeg is not installed")
        }
    }
    runtime.exec("optipng -v").also { it.waitFor() }.exitValue().let {
        if (it != 0) {
            throw IllegalStateException("OptiPNG is not installed")
        }
    }
    runtime.exec("dwebp -h").also { it.waitFor() }.exitValue().let {
        if (it != 0) {
            throw IllegalStateException("WebP tools are not installed")
        }
    }
    runtime.exec("lottie_convert.py -h").also { it.waitFor() }.exitValue().let {
        if (it != 0) {
            throw IllegalStateException("Lottie tools are not installed")
        }
    }
}

fun main(args: Array<String>) {
    ensureExternalTools()
    runApplication<MyStickersBotApplication>(*args)
}
