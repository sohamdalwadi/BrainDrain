package com.dalwadi.braindrain.core.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dalwadi.braindrain.core.model.AppPlatform

/**
 * Open-source extension interface for detecting short-form feeds across various apps.
 * Contributors can implement this interface to support additional apps (e.g., TikTok, Reddit).
 */
interface PlatformDetector {
    val platform: AppPlatform
    val targetPackage: String

    /**
     * Determines whether the active screen corresponds to a short-form video feed.
     */
    fun isShortFormFeed(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean

    /**
     * Checks if the received accessibility scroll event is a forward vertical swipe (new reel).
     */
    fun isForwardScrollEvent(event: AccessibilityEvent): Boolean
}
