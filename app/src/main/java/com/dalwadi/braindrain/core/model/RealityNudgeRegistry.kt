package com.dalwadi.braindrain.core.model

object RealityNudgeRegistry {

    private val lightNudges = listOf(
        "Another 15-second masterpiece of human evolution consumed.",
        "The algorithm thanks you for your cooperation.",
        "Dopamine drip detected. Still fully in control?",
        "15 seconds gone that will never return to your life.",
        "Your attention is being harvested and auctioned in real-time."
    )

    private val moderateNudges = listOf(
        "Dopamine receptors are politely requesting a breather.",
        "Swipe after swipe. Notice anything memorable yet?",
        "Your future self is watching this session, mildly unimpressed.",
        "Synthetic novelty detected. Reality is waiting outside.",
        "The feed never ends. Your waking hours, however, do.",
        "You are trading non-renewable focus for algorithmic noise."
    )

    private val heavyNudges = listOf(
        "Cognitive friction rising. Attention reserve depleted by 75%.",
        "The algorithm knows your weaknesses. Do not let it win.",
        "Is this clip worth trading your remaining evening for?",
        "Sensory overload imminent. Step back before inertia takes over.",
        "Your brain is burning fuel on synthetic entertainment.",
        "The loop only breaks when you consciously decide to break it."
    )

    private val criticalNudges = listOf(
        "Daily ceiling reached. Further scrolling is pure inertia.",
        "Will watching further help you? Your day is slipping away.",
        "You have hit your quota. Put the phone face down and walk away.",
        "Zero value added past this point. Break the loop now."
    )

    fun getNudge(count: Int, limit: Int): String? {
        if (count <= 0 || limit <= 0) return null

        val ratio = count.toFloat() / limit.toFloat()

        val list = when {
            ratio >= 1.0f -> criticalNudges
            ratio >= 0.75f -> heavyNudges
            ratio >= 0.45f -> moderateNudges
            ratio >= 0.20f && count % 5 == 0 -> lightNudges
            else -> return null
        }

        val index = (count / 3) % list.size
        return list[index]
    }
}
