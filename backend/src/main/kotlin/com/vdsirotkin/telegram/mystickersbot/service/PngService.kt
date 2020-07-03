package com.vdsirotkin.telegram.mystickersbot.service

import com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException
import io.github.biezhi.webp.WebpIO
import org.springframework.stereotype.Service
import ru.sokomishalov.commons.core.log.Loggable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

@Service
class PngService {

    /**
     * Method for converting webp to png, also treats 512kb telegram limitation
     *
     * @return PNG file path
     * @throws com.vdsirotkin.telegram.mystickersbot.exception.PngNotCreatedException when some IO exception occurres or can't fit telegram limitations
     */
    fun convertToPng(webpFile: File): File {
        val pngFilePath = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".png")
        val pngFile = pngFilePath.toFile()
        WebpIO.create().toNormalImage(webpFile, pngFile)
        if (tooBigSize(pngFilePath)) {
            reducePngSize(webpFile, pngFilePath.toFile())
            if (tooBigSize(pngFilePath)) {
                throw PngNotCreatedException()
            }
        }
        return pngFilePath.toFile()
    }

    private fun reducePngSize(webpFile: File, pngFilePath: File) {
        val runtime = Runtime.getRuntime()
        var command = "ffmpeg -y -i ${webpFile.absolutePath} ${pngFilePath.absolutePath}"
        logDebug("Executing system command: $command")
        var inputStream = runtime.exec(command).inputStream
        var reader = BufferedReader(InputStreamReader(inputStream))
        reader.lines().collect(Collectors.joining("\n")).also { logDebug("Result: $it") }

        command = "optipng ${pngFilePath.absolutePath}"
        logDebug("Executing system command: $command")
        inputStream = runtime.exec(command).inputStream
        reader = BufferedReader(InputStreamReader(inputStream))
        reader.lines().collect(Collectors.joining("\n")).also { logDebug("Result: $it") }
    }

    private fun tooBigSize(pngFilePath: Path?) = (Files.size(pngFilePath) / 1024) > 500

    companion object : Loggable

}