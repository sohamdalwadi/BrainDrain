package com.dalwadi.braindrain.core.detector

object DetectorRegistry {
    private val detectors = mutableMapOf<String, PlatformDetector>()

    init {
        register(InstagramDetector())
        register(YouTubeShortsDetector())
    }

    fun register(detector: PlatformDetector) {
        detectors[detector.targetPackage] = detector
    }

    fun getDetector(packageName: String?): PlatformDetector? {
        if (packageName == null) return null
        return detectors[packageName]
    }

    fun isSupportedPackage(packageName: String?): Boolean {
        if (packageName == null) return false
        return detectors.containsKey(packageName)
    }

    fun allDetectors(): List<PlatformDetector> = detectors.values.toList()
}
