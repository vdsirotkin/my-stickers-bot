package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import com.vdsirotkin.telegram.mystickersbot.util.TEMP_FILE_PREFIX
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.client.WebClient
import ru.sokomishalov.commons.core.log.Loggable
import ru.sokomishalov.commons.core.reactor.await
import java.io.File
import java.nio.file.Files

@Component
class FileHelper(
        private val webClient: WebClient = WebClient.create()
) {

    suspend fun downloadFile(bot: TelegramBot, fileId: String): File {
        val tempFile = withContext(Dispatchers.IO) {
            Files.createTempFile(TEMP_FILE_PREFIX, "")
        }
        downloadFile(bot, fileId, tempFile.toFile())
        return tempFile.toFile()
    }

    suspend fun downloadFile(bot: TelegramBot, fileId: String, toFile: File) {
        val fileResp = bot.executeAsync(GetFile(fileId))
        logger.info(fileResp.toString())
        val tempFile = toFile.toPath()
        val fullUrl = bot.getFullFilePath(fileResp.file())
        webClient
                .get()
                .uri(fullUrl)
                .exchange()
                .flatMapMany { it.body(BodyExtractors.toDataBuffers()) }
                .let {
                    DataBufferUtils.write(it, tempFile)
                }
                .await()
    }

    companion object : Loggable

}
