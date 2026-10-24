package com.dalwadi.braindrain.core.ota

data class OtaUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val channel: String = "stable"
)
