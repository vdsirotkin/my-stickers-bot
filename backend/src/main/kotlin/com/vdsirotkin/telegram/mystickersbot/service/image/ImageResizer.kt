package com.vdsirotkin.telegram.mystickersbot.service.image

import com.vdsirotkin.telegram.mystickersbot.handler.photo.PhotoHandler
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.imaging.Imaging
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import kotlin.math.roundToInt

@Service
class ImageResizer {

    fun resizeImage(file: File): File {
        val metadata = Imaging.getImageInfo(file)
        val width = metadata.width
        val height = metadata.height
        val newImage = Files.createTempFile("com.vdsirotkin.telegram.mystickersbot-", ".png").toFile()
        val dimensions = when {
            height > width -> { // vertical
                val newHeight = 512
                val coefficient = newHeight.toDouble() / height.toDouble()
                val newWidth = (width * coefficient).roundToInt()
                PhotoHandler.Dimensions(newWidth, newHeight)
            }
            width > height -> { // horizontal
                val newWidth = 512
                val coefficient = newWidth.toDouble() / width.toDouble()
                val newHeight = (height * coefficient).roundToInt()
                PhotoHandler.Dimensions(newWidth, newHeight)
            }
            else -> { // square
                val newWidth = 512
                val newHeight = 512
                PhotoHandler.Dimensions(newWidth, newHeight)
            }
        }
        val (newWidth, newHeight) = dimensions
        PhotoHandler.logger.info("Old dimensions: w$width,h$height, new dimesions: w$newWidth,h$newHeight")
        Thumbnails.of(file).forceSize(newWidth, newHeight).toFile(newImage)
        return newImage
    }

}
