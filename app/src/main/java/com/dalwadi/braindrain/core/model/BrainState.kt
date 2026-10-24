package com.dalwadi.braindrain.core.model

import com.dalwadi.braindrain.R

enum class BrainState(
    val title: String,
    val subtitle: String,
    val vectorResId: Int,
    val accentColorHex: Long
) {
    OPTIMAL(
        title = "Optimal State",
        subtitle = "Cognitive reserve is high. Mind is focused and clear.",
        vectorResId = R.drawable.ic_brain_optimal,
        accentColorHex = 0xFF10B981
    ),
    FATIGUE(
        title = "Attention Fatigue",
        subtitle = "Dopamine saturation detected. Consider a short pause.",
        vectorResId = R.drawable.ic_brain_fatigue,
        accentColorHex = 0xFFF59E0B
    ),
    DEPLETED(
        title = "Attention Depleted",
        subtitle = "High scroll saturation. Brain drain threshold exceeded.",
        vectorResId = R.drawable.ic_brain_depleted,
        accentColorHex = 0xFFEF4444
    );

    companion object {
        fun fromScrollCount(count: Int, limit: Int = 50): BrainState {
            val fatigueThreshold = (limit * 0.4).toInt().coerceAtLeast(15)
            return when {
                count < fatigueThreshold -> OPTIMAL
                count < limit -> FATIGUE
                else -> DEPLETED
            }
        }
    }
}
