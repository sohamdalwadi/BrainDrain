package com.dalwadi.braindrain.core.detector

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dalwadi.braindrain.core.model.AppPlatform

class InstagramDetector : PlatformDetector {
    override val platform: AppPlatform = AppPlatform.INSTAGRAM
    override val targetPackage: String = "com.instagram.android"

    private var lastScrollTime = 0L
    private val debounceWindowMs = 1800L

    override fun isShortFormFeed(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean {
        if (event.packageName?.toString() != targetPackage) return false
        if (isCommentsOpen(rootNode)) return false

        if (rootNode != null) {
            val reelsTabSelected = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_tab")
                .any { it.isSelected }
            if (reelsTabSelected) return true

            val hasReelsView = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_video_container").isNotEmpty() ||
                    rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/layout_clips_swipe_refresh").isNotEmpty() ||
                    rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_viewer_view_pager").isNotEmpty()
            if (hasReelsView) return true
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

        // 1. Reject if comments are open in the active window
        if (isCommentsOpen(rootNode)) {
            return false
        }

        // 2. Reject horizontal scrolls (stories tray, carousels) or backwards scrolls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Forward scroll in reels: swiping up moves content down, scrollDeltaY > 0
            if (event.scrollDeltaY <= 40) {
                return false
            }
        }

        // 3. Inspect the scrolling source view ID
        val sourceId = event.source?.viewIdResourceName?.lowercase() ?: ""
        val sourceClass = event.source?.className?.toString() ?: ""

        // Reject comments scrolling
        if (sourceId.contains("comment") || sourceId.contains("thread") || sourceId.contains("composer")) {
            return false
        }

        // Reject stories tray / header / tabs / explore
        if (sourceId.contains("reels_tray") || sourceId.contains("stories") ||
            sourceId.contains("tab_bar") || sourceId.contains("action_bar") ||
            sourceId.contains("search") || sourceId.contains("explore")
        ) {
            return false
        }

        if (sourceClass.contains("HorizontalScrollView", ignoreCase = true)) {
            return false
        }

        lastScrollTime = now
        return true
    }

    private fun isCommentsOpen(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        val commentNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/layout_comment_thread")
        if (commentNodes.isNotEmpty()) return true

        val composerNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/layout_comment_composer")
        if (composerNodes.isNotEmpty()) return true

        val commentsList = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/comments_recycler_view")
        if (commentsList.isNotEmpty()) return true

        return false
    }
}

