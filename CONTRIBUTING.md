# Contributing to BrainDrain

Thank you for your interest in contributing to **BrainDrain**! This project is maintained and architected by **Soham Dalwadi** and is open to community contributions under our shared commitment to privacy, simplicity, and zero monetization.

---

## Guiding Principles

1. **No Paywalls or Subscriptions:** Any PR attempting to add in-app purchases, paid features, telemetry, or user tracking will be closed.
2. **Strict Zero-Emoji Policy:** To preserve the aesthetic, minimalist, and technical identity of BrainDrain, do not use emojis in UI text, code comments, logs, or documentation. Use vector drawables, SVGs, or Material Design icons instead.
3. **Privacy First:** All detection and counting logic must remain entirely on-device.

---

## Adding Support for a New Platform

Adding detection for a new short-form platform (e.g., TikTok, Reddit, Snapchat) requires just 3 steps:

### 1. Add Platform to `AppPlatform.kt`
Define your platform with an ID, display name, target Android package name, and vector icon resource:
```kotlin
TIKTOK(
    id = "tiktok",
    displayName = "TikTok",
    packageName = "com.zhiliaoapp.musically",
    vectorResId = R.drawable.ic_tiktok
)
```

### 2. Implement `PlatformDetector`
Create a new detector class in `com.dalwadi.braindrain.core.detector`:
```kotlin
class TikTokDetector : PlatformDetector {
    override val platform: AppPlatform = AppPlatform.TIKTOK
    override val targetPackage: String = "com.zhiliaoapp.musically"

    override fun isShortFormFeed(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean {
        // Inspect event class or node IDs specific to short-form player
        return true
    }

    override fun isForwardScrollEvent(event: AccessibilityEvent): Boolean {
        // Debounce vertical swipe scroll
        return event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
    }
}
```

### 3. Register with `DetectorRegistry`
In `DetectorRegistry.kt`:
```kotlin
init {
    register(InstagramDetector())
    register(YouTubeShortsDetector())
    register(TikTokDetector()) // Registered
}
```

---

## Submitting Pull Requests

1. Fork the repository and create your branch from `main`.
2. Ensure the code compiles cleanly:
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew compileDebugKotlin
   ```
3. Test your changes on a physical device or emulator.
4. Open a Pull Request with a clear description of the new detector or feature.
