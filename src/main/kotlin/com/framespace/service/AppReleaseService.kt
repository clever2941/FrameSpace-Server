package com.framespace.service

import com.framespace.dto.AppUpdateCheckDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class AppReleaseService(
    @Value("\${app.release.version-code:1}") private val latestVersionCode: Int,
    @Value("\${app.release.version-name:1.0.0}") private val latestVersionName: String,
    @Value("\${app.release.apk-filename:framespace-latest.apk}") private val apkFilename: String,
    @Value("\${app.release.release-notes:优化体验与修复问题}") private val releaseNotes: String,
    @Value("\${app.release.force-update:false}") private val forceUpdate: Boolean,
    @Value("\${app.public-base-url:http://localhost:8080}") private val publicBaseUrl: String,
    @Value("\${app.upload-dir:uploads}") private val uploadDir: String
) {

    fun checkUpdate(clientVersionCode: Int): AppUpdateCheckDto {
        val apkPath = resolveApkPath()
        val apkExists = Files.exists(apkPath)
        val hasUpdate = apkExists && clientVersionCode < latestVersionCode
        val base = publicBaseUrl.trimEnd('/')
        return AppUpdateCheckDto(
            hasUpdate = hasUpdate,
            versionCode = latestVersionCode,
            versionName = latestVersionName,
            releaseNotes = releaseNotes,
            forceUpdate = forceUpdate && hasUpdate,
            downloadUrl = "$base/api/app/update/download",
            apkSizeBytes = if (apkExists) Files.size(apkPath) else 0L
        )
    }

    fun openApkStream(): Pair<InputStream, Long>? {
        val path = resolveApkPath()
        if (!Files.exists(path)) return null
        return Files.newInputStream(path) to Files.size(path)
    }

    fun resolveApkPath(): Path = Paths.get(uploadDir, "releases", apkFilename)
}
