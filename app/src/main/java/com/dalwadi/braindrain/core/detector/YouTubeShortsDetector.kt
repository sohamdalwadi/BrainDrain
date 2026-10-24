package com.dalwadi.braindrain.core.detector

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dalwadi.braindrain.core.model.AppPlatform

class YouTubeShortsDetector : PlatformDetector {
    override val platform: AppPlatform = AppPlatform.YOUTUBE_SHORTS
    override val targetPackage: String = "com.google.android.youtube"

    private var lastScrollTime = 0L
    private val debounceWindowMs = 1800L

    override fun isShortFormFeed(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean {
        if (event.packageName?.toString() != targetPackage) return false
        if (isCommentsOpen(rootNode)) return false

        if (rootNode != null) {
            val hasReelPlayer = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_player_page_view").isNotEmpty() ||
                    rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_recycler").isNotEmpty()
            if (hasReelPlayer) return true
        }

        return false
    }

    override fun isForwardScrollEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastScrollTime < debounceWindowMs) {
            return false
        }

        // 1. Reject if comments / engagement panels are open
        if (isCommentsOpen(rootNode)) {
            return false
        }

        // 2. Reject horizontal / backwards scrolls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (event.scrollDeltaY <= 40) {
                return false
            }
        }

        // 3. Reject comment / sheet source views
        val sourceId = event.source?.viewIdResourceName?.lowercase() ?: ""
        if (sourceId.contains("comment") || sourceId.contains("bottom_sheet") || sourceId.contains("engagement_panel")) {
            return false
        }

        lastScrollTime = now
        return true
    }

    private fun isCommentsOpen(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        val commentNodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/comments_sheet")
        if (commentNodes.isNotEmpty()) return true
        val panelNodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/engagement_panel")
        if (panelNodes.isNotEmpty()) return true
        return false
    }
}

