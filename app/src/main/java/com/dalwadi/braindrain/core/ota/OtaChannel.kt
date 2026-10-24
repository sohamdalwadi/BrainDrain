package com.dalwadi.braindrain.core.ota

enum class OtaChannel(val id: String, val displayName: String, val manifestUrl: String) {
    STABLE(
        id = "stable",
        displayName = "Stable (main)",
        manifestUrl = "https://github.com/sohamdalwadi/BrainDrain/releases/latest/download/latest.json"
    ),
    BETA(
        id = "beta",
        displayName = "Beta (develop)",
        manifestUrl = "https://github.com/sohamdalwadi/BrainDrain/releases/download/beta-latest/latest-beta.json"
    )
}

