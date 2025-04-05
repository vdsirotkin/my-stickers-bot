package com.vdsirotkin.telegram.mystickersbot.service

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetFile
import com.vdsirotkin.telegram.mystickersbot.util.TEMP_FILE_PREFIX
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import ru.sokomishalov.commons.core.log.Loggable
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.Executors

@Component
class FileHelper {
    private val httpClient = HttpClient.newBuilder()
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .connectTimeout(Duration.ofSeconds(30))
        .build()

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
        val fullUrl = bot.getFullFilePath(fileResp.file())
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create(fullUrl))
            .GET()
            .build()
            
        withContext(Dispatchers.IO) {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            response.body().use { input ->
                Files.copy(input, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    companion object : Loggable
}
