package com.framespace.dto

data class AppUpdateCheckDto(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val forceUpdate: Boolean,
    val downloadUrl: String,
    val apkSizeBytes: Long = 0
)
