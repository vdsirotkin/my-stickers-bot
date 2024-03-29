package com.vdsirotkin.telegram.mystickersbot.service.image

import com.vdsirotkin.telegram.mystickersbot.util.PNG_SUFFIX
import com.vdsirotkin.telegram.mystickersbot.util.TEMP_FILE_PREFIX
import com.vdsirotkin.telegram.mystickersbot.util.withTempFile
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.imaging.Imaging
import org.springframework.stereotype.Service
import ru.sokomishalov.commons.core.log.Loggable
import java.io.File
import java.nio.file.Files
import kotlin.math.roundToInt

@Service
class ImageResizer(
        private val pngService: PngService
) {

    private val MIME_PNG = "image/png"

    suspend fun resizeImage(file: File): File {
        val metadata = Imaging.getImageInfo(file)
        val width = metadata.width
        val height = metadata.height
        if (((width == 512 && height <= 512) || (height == 512 && width <= 512)) && metadata.mimeType == MIME_PNG && !pngService.tooBigSize(file.toPath())) {
            return file // already ok
        }
        val newImage = Files.createTempFile(TEMP_FILE_PREFIX, PNG_SUFFIX).toFile()
        val dimensions = when {
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
        val (newWidth, newHeight) = dimensions
        logger.info("Old dimensions: w$width,h$height, new dimesions: w$newWidth,h$newHeight")
        Thumbnails.of(file).forceSize(newWidth, newHeight).toFile(newImage)
        if (pngService.tooBigSize(newImage.toPath())) {
            withTempFile(Files.createTempFile(TEMP_FILE_PREFIX, ".jpg").toFile()) { newJpgImage ->
                Thumbnails.of(file).forceSize(newWidth, newHeight).outputFormat("jpg").toFile(newJpgImage)
                pngService.jpegToPng(newJpgImage, newImage)
            }
        }
        file.delete()
        return newImage
    }

    companion object : Loggable

    data class Dimensions(val width: Int, val height: Int)

}
