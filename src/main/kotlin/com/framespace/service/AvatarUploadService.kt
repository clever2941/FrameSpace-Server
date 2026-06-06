package com.framespace.service

import com.framespace.dto.AvatarUploadDto
import com.framespace.mapper.UserMapper
import com.framespace.util.AvatarImageUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

@Service
class AvatarUploadService(
    private val userMapper: UserMapper,
    private val imageUrlService: ImageUrlService,
    @Value("\${app.upload-dir:uploads}") private val uploadDir: String
) {

    private val resizedCache = ConcurrentHashMap<String, Pair<ByteArray, String>>()

    fun uploadAvatar(userId: Long, file: MultipartFile): AvatarUploadDto {
        require(!file.isEmpty) { "请选择图片文件" }
        val contentType = file.contentType?.lowercase().orEmpty()
        require(contentType.startsWith("image/")) { "仅支持图片格式" }
        require(file.size <= 3 * 1024 * 1024) { "图片不能超过 3MB" }

        val image = ImageIO.read(file.inputStream)
            ?: throw IllegalArgumentException("无法解析图片")
        val resized = AvatarImageUtil.scaleToMaxPx(image, STORED_MAX_PX)
        val jpegBytes = AvatarImageUtil.encodeJpeg(resized)

        val relativePath = "avatars/u${userId}_${System.currentTimeMillis()}.jpg"
        val target = resolveUploadPath(relativePath)
        Files.createDirectories(target.parent)
        Files.write(target, jpegBytes)

        val user = userMapper.selectById(userId) ?: throw IllegalArgumentException("用户不存在")
        user.avatarUrl = relativePath
        userMapper.updateById(user)
        resizedCache.keys.removeIf { it.startsWith("$relativePath:") }

        val publicUrl = imageUrlService.buildAvatarUrl(relativePath)
            ?: throw IllegalStateException("头像地址生成失败")
        return AvatarUploadDto(avatarUrl = publicUrl)
    }

    fun readAvatar(relativePath: String, maxPx: Int? = null): Pair<ByteArray, String>? {
        val safe = sanitizePath(relativePath) ?: return null
        val serveMax = maxPx?.coerceIn(32, 512) ?: DEFAULT_SERVE_MAX_PX
        val cacheKey = "$safe:$serveMax"
        resizedCache[cacheKey]?.let { return it }

        val path = resolveUploadPath(safe)
        if (!Files.exists(path)) return null
        val image = ImageIO.read(path.toFile()) ?: return null
        val scaled = AvatarImageUtil.scaleToMaxPx(image, serveMax)
        val payload = AvatarImageUtil.encodeJpeg(scaled) to "image/jpeg"
        resizedCache[cacheKey] = payload
        return payload
    }

    private fun sanitizePath(relativePath: String): String? {
        val safe = relativePath.trim().replace("\\", "/")
        if (safe.contains("..") || !safe.startsWith("avatars/")) return null
        return safe
    }

    private fun resolveUploadPath(relativePath: String): Path =
        Paths.get(uploadDir).resolve(relativePath).normalize()

    companion object {
        private const val STORED_MAX_PX = 400
        private const val DEFAULT_SERVE_MAX_PX = 128
    }
}
