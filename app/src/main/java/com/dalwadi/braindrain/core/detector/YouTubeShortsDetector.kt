package com.dalwadi.braindrain.core.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dalwadi.braindrain.core.model.AppPlatform

class YouTubeShortsDetector : PlatformDetector {
    override val platform: AppPlatform = AppPlatform.YOUTUBE_SHORTS
    override val targetPackage: String = "com.google.android.youtube"

    private var lastScrollTime = 0L
    private val debounceWindowMs = 800L

    override fun isShortFormFeed(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean {
        // Active when inside YouTube
        return event.packageName?.toString() == targetPackage
    }

    override fun isForwardScrollEvent(event: AccessibilityEvent): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastScrollTime < debounceWindowMs) {
            return false
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            lastScrollTime = now
            return true
        }

        return false
    }
}
