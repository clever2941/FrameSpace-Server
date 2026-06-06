package com.framespace.controller

import com.framespace.common.Result
import com.framespace.dto.AppUpdateCheckDto
import com.framespace.service.AppReleaseService
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.math.min

@RestController
@RequestMapping("/api/app/update")
class AppUpdateController(
    private val appReleaseService: AppReleaseService
) {

    @GetMapping("/check")
    fun checkUpdate(
        @RequestParam(value = "versionCode", defaultValue = "0") versionCode: Int
    ): Result<AppUpdateCheckDto> {
        return Result.success(appReleaseService.checkUpdate(versionCode))
    }

    @GetMapping("/download")
    fun downloadApk(
        @RequestHeader(value = HttpHeaders.RANGE, required = false) range: String?
    ): ResponseEntity<StreamingResponseBody> {
        val path = appReleaseService.resolveApkPath()
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        val size = Files.size(path)
        val byteRange = if (range.isNullOrBlank()) {
            ByteRange(0, size - 1)
        } else {
            parseRange(range, size) ?: return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */$size")
                .build()
        }
        val rangeStart = byteRange.start
        val rangeEnd = byteRange.endInclusive
        val contentLength = rangeEnd - rangeStart + 1

        val body = StreamingResponseBody { output ->
            Files.newInputStream(path).use { stream ->
                if (rangeStart > 0) {
                    stream.skip(rangeStart)
                }
                val buffer = ByteArray(64 * 1024)
                var remaining = contentLength
                while (remaining > 0) {
                    val toRead = min(buffer.size.toLong(), remaining).toInt()
                    val read = stream.read(buffer, 0, toRead)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }

        val common = ResponseEntity.ok()
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"framespace-latest.apk\"")
            .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
            .contentLength(contentLength)

        return if (!range.isNullOrBlank()) {
            ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"framespace-latest.apk\"")
                .header(HttpHeaders.CONTENT_RANGE, "bytes $rangeStart-$rangeEnd/$size")
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .contentLength(contentLength)
                .body(body)
        } else {
            common.body(body)
        }
    }

    private data class ByteRange(val start: Long, val endInclusive: Long)

    private fun parseRange(range: String, fileSize: Long): ByteRange? {
        if (!range.startsWith("bytes=")) return null
        val spec = range.removePrefix("bytes=").trim()
        val dash = spec.indexOf('-')
        if (dash < 0) return null
        val startText = spec.substring(0, dash).trim()
        val endText = spec.substring(dash + 1).trim()
        val start = startText.toLongOrNull() ?: return null
        if (start < 0 || start >= fileSize) return null
        val endInclusive = if (endText.isBlank()) {
            fileSize - 1
        } else {
            endText.toLongOrNull() ?: return null
        }.coerceAtMost(fileSize - 1)
        if (endInclusive < start) return null
        return ByteRange(start, endInclusive)
    }
}
