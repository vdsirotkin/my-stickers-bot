package com.vdsirotkin.telegram.mystickersbot.service

import com.vdsirotkin.telegram.mystickersbot.util.downloadFileAsync
import com.vdsirotkin.telegram.mystickersbot.util.executeAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.GetFile
import ru.sokomishalov.commons.core.log.Loggable
import java.io.File
import java.nio.file.Files

@Component
class FileHelper {

    suspend fun downloadFile(bot: DefaultAbsSender, fileId: String): File {
        val file = bot.executeAsync(GetFile().apply { this.fileId = fileId })
        logger.info(file.toString())
        return bot.downloadFileAsync(file.filePath)
    }

    suspend fun downloadFile(bot: DefaultAbsSender, fileId: String, toFile: File) {
        val file = downloadFile(bot, fileId)
        withContext(Dispatchers.IO) {
            Files.copy(file.toPath(), toFile.toPath())
        }
    }

    companion object : Loggable

}
