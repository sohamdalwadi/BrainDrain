# BrainDrain

**Architect & Lead Developer:** Soham Dalwadi  
**License:** MIT / Apache-2.0 (100% Free & Open Source)  
**Philosophy:** Zero Subscriptions. Zero Paywalls. Zero Ads. 100% Offline & Private.  
**Design Standard:** Pure Vector / SVG Aesthetic (Strictly Zero Emojis).

---

## Overview

**BrainDrain** is an open-source Android digital wellbeing and anti-doomscrolling HUD utility. Unlike commercial apps that lock screen limits and counters behind expensive weekly subscriptions, BrainDrain is built from the ground up to be:

* **100% Free Forever:** No pro tiers, no locked features, no trial periods.
* **Completely Private & Local:** All accessibility event analysis and swipe counts happen strictly on device. No analytics, no tracking, no internet connection required.
* **Pure Vector Aesthetics:** Crisp, high-contrast dark theme utilizing geometric vector paths and SVGs with a strict zero-emoji design philosophy.
* **Modular & Extensible:** Open to community contributions with a pluggable `PlatformDetector` interface.

---

## Features

* **Real-Time Scroll Tracking:** Accurately counts short-form video consumption across **Instagram Reels** and **YouTube Shorts** with debounce and inertia filtering.
* **Floating Live Pill HUD:** Non-intrusive, camera-framing floating overlay (Dynamic Island style) displaying real-time scroll counts and fatigue state directly over supported apps.
* **Neural State Index (Vector Engine):** Dynamic geometric brain state visualization reflecting dopamine reserve and attention fatigue:
  * **Optimal State:** High cognitive reserve, clear focus.
  * **Attention Fatigue:** Dopamine saturation warning, subtle attention nudge.
  * **Attention Depleted:** Daily threshold exceeded, urging a mental breather.
* **In-App OTA Updates:** Native over-the-air updater supporting **Stable (`main`)** and **Beta (`develop`)** channels with direct APK download & installation.
* **Direct GitHub Issue Reporting:** In-app one-tap bug report and feature request launcher to file issues directly on GitHub.
* **Custom Daily Caps:** Configurable daily limits with soft haptic ticks and feedback.
* **Zero Overhead:** Built with Kotlin, Jetpack Compose, Material 3, and DataStore Preferences.

---

## Technical Architecture

```
app/src/main/java/com/dalwadi/braindrain/
├── BrainDrainApp.kt                      # Application entry point
├── core/
│   ├── data/
│   │   └── PreferencesRepository.kt      # DataStore-backed offline storage
│   ├── detector/
│   │   ├── PlatformDetector.kt           # Extensible detector interface
│   │   ├── InstagramDetector.kt          # Instagram Reels detector
│   │   ├── YouTubeShortsDetector.kt      # YouTube Shorts detector
│   │   └── DetectorRegistry.kt           # Central detector registry
│   └── model/
│       ├── AppPlatform.kt                # Platform declarations
│       ├── BrainState.kt                 # Brain state vector & calculation model
│       └── ScrollSession.kt              # Session state model
├── overlay/
│   └── OverlayManager.kt                 # Floating HUD WindowManager engine
├── service/
│   └── BrainDrainAccessibilityService.kt # Core swipe detection engine
└── ui/
    ├── MainActivity.kt                   # Host activity with permission handlers
    ├── DashboardViewModel.kt             # Reactive MVI/MVVM ViewModel
    ├── DashboardScreen.kt                # Jetpack Compose Vector Dashboard
    └── theme/                            # Minimalist dark theme & colors
```

---

## Release Channels & Branching Strategy

BrainDrain strictly separates stable and experimental builds through continuous integration:

| Channel | Branch | APK Flavor | Description |
| :--- | :--- | :--- | :--- |
| **Stable** | `main` | Production Release | Tested, rock-solid builds tagged `vX.Y.Z` on GitHub Releases. |
| **Beta** | `develop` | Pre-Release | Bleeding-edge features, early detector additions, and experimental UX. |

Every push to `main` triggers automated CI that verifies the build and publishes a GitHub Release tagged `vX.Y.Z` alongside the updated manifest `latest.json`.
Every push to `develop` triggers automated CI that verifies and publishes a GitHub Pre-Release alongside `latest-beta.json`.

---

## Issue Reporting

Encountered a bug or have an idea for a feature? You can file issues directly from within the app (via the in-app "Report an Issue" button) or manually via GitHub Issues:
* **Bug Report:** Use our structured bug report template detailing device model, Android version, and logs.
* **Feature Request / Detector Proposal:** Propose support for new apps or platforms.

---

## Building from Source

### Prerequisites
* JDK 17+ (or Android Studio's bundled JBR)
* Android SDK 34 / 35

### Build Commands
```powershell
# Set Java Home (example using Android Studio's JBR)
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

# Kotlin Compile Check
.\gradlew compileDebugKotlin

# Assemble Debug APK
.\gradlew assembleDebug
```

---

## Contributing

We welcome contributions from the community! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to add support for additional platforms (such as TikTok, Facebook Reels, or Reddit) and maintain the vector design standard.
