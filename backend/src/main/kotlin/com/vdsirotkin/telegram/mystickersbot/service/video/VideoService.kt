package com.vdsirotkin.telegram.mystickersbot.service.video

import com.pengrad.telegrambot.model.Animation
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer
import com.vdsirotkin.telegram.mystickersbot.service.image.ImageResizer.Dimensions
import com.vdsirotkin.telegram.mystickersbot.util.TEMP_FILE_PREFIX
import com.vdsirotkin.telegram.mystickersbot.util.execWithLog
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import kotlin.math.roundToInt

@Service
class VideoService {

    suspend fun convertToWebm(animation: Animation, mp4File: File): File {
        val height = animation.height()
        val width = animation.width()
        var dimensions: Dimensions? = null
        if ((width == 512 && height <= 512) || (height == 512 && width <= 512)) {
            dimensions = Dimensions(width, height) // already ok
        }
        if (dimensions == null) {
            dimensions = when {
                height > width -> { // vertical
                    val newHeight = 512
                    val coefficient = newHeight.toDouble() / height.toDouble()
                    val newWidth = (width * coefficient).roundToInt()
                    Dimensions(newWidth, newHeight)
                }
                width > height -> { // horizontal
                    val newWidth = 512
                    val coefficient = newWidth.toDouble() / width.toDouble()
                    val newHeight = (height * coefficient).roundToInt()
                    Dimensions(newWidth, newHeight)
                }
                else -> { // square
                    val newWidth = 512
                    val newHeight = 512
                    Dimensions(newWidth, newHeight)
                }
            }
        }
        val (newWidth, newHeight) = dimensions
        ImageResizer.logger.info("Old dimensions: w$width,h$height, new dimesions: w$newWidth,h$newHeight")
        return withTempFile(mp4File) {
            val webmFile = Files.createTempFile(TEMP_FILE_PREFIX, ".webm").toFile()
            webmFile.delete()
            execWithLog("ffmpeg -i ${it.absolutePath} -c:v libvpx-vp9 -c:a libopus -vf scale=${newWidth}:${newHeight} -to 00:00:02.8 ${webmFile.absolutePath}")
            webmFile
        }
    }

}