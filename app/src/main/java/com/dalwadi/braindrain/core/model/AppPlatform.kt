package com.dalwadi.braindrain.core.model

import com.dalwadi.braindrain.R

enum class AppPlatform(
    val id: String,
    val displayName: String,
    val packageName: String,
    val vectorResId: Int
) {
    INSTAGRAM(
        id = "instagram",
        displayName = "Instagram Reels",
        packageName = "com.instagram.android",
        vectorResId = R.drawable.ic_instagram
    ),
    YOUTUBE_SHORTS(
        id = "youtube_shorts",
        displayName = "YouTube Shorts",
        packageName = "com.google.android.youtube",
        vectorResId = R.drawable.ic_youtube_shorts
    );

    companion object {
        fun fromPackage(packageName: String): AppPlatform? {
            return entries.firstOrNull { it.packageName == packageName }
        }
    }
}
