package com.framespace.util

import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object AvatarImageUtil {

    fun scaleToMaxPx(source: BufferedImage, maxPx: Int): BufferedImage {
        val safeMax = maxPx.coerceIn(32, 1024)
        val width = source.width
        val height = source.height
        if (width <= safeMax && height <= safeMax) return source

        val scale = minOf(safeMax.toDouble() / width, safeMax.toDouble() / height)
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        val imageType = if (source.type == BufferedImage.TYPE_CUSTOM) BufferedImage.TYPE_INT_RGB else source.type
        val scaled = BufferedImage(targetW, targetH, imageType)
        val graphics = scaled.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(source.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH), 0, 0, null)
        graphics.dispose()
        return scaled
    }

    fun readImage(bytes: ByteArray): BufferedImage? =
        runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()

    fun encodeJpeg(image: BufferedImage, quality: Float = 0.82f): ByteArray {
        val rgb = if (image.type == BufferedImage.TYPE_INT_RGB) {
            image
        } else {
            val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
            val graphics = converted.createGraphics()
            graphics.drawImage(image, 0, 0, null)
            graphics.dispose()
            converted
        }
        val output = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        val writer = writers.next()
        try {
            val param = writer.defaultWriteParam
            if (param.canWriteCompressed()) {
                param.compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                param.compressionQuality = quality.coerceIn(0.5f, 0.95f)
            }
            val stream = ImageIO.createImageOutputStream(output)
            writer.output = stream
            writer.write(null, javax.imageio.IIOImage(rgb, null, null), param)
            stream.close()
        } finally {
            writer.dispose()
        }
        return output.toByteArray()
    }
}
