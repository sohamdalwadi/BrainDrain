package com.dalwadi.braindrain.core.model

data class ScrollSession(
    val platform: AppPlatform,
    val sessionCount: Int = 0,
    val dailyCount: Int = 0,
    val dailyLimit: Int = 50,
    val isLimitExceeded: Boolean = false,
    val lastScrollTimestamp: Long = System.currentTimeMillis()
)
