package com.dalwadi.braindrain.core.ota

enum class OtaChannel(val id: String, val displayName: String, val branch: String, val manifestFile: String) {
    STABLE("stable", "Stable (main)", "main", "latest.json"),
    BETA("beta", "Beta (develop)", "develop", "latest-beta.json")
}
