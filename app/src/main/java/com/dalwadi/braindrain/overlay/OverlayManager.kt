package com.dalwadi.braindrain.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dalwadi.braindrain.R
import com.dalwadi.braindrain.core.model.BrainState
import com.dalwadi.braindrain.core.model.RealityNudgeRegistry

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    // 1. Ultra-Compact Floating Pill HUD
    private var pillView: View? = null
    private var isPillAttached = false
    private var iconView: ImageView? = null
    private var countNumberView: TextView? = null
    private var limitBadgeView: TextView? = null
    private var pillBackground: GradientDrawable? = null
    private var particleEmitter: ParticleEmitterView? = null
    private var currentPillParams: WindowManager.LayoutParams? = null

    // Pill dragging state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var lastCount = -1
    private var lastNudgeTime = 0L

    // 2. Standalone Non-Blocking Nudge Banner Window
    private var nudgeBannerView: View? = null
    private var isNudgeAttached = false

    // 3. Center Reality Check Intervention Window
    private var realityCheckView: View? = null
    private var isRealityCheckAttached = false
    private var lastInterventionDismissCount = -1
    private var onExitToHomeAction: (() -> Unit)? = null

    fun setOnExitToHomeListener(action: () -> Unit) {
        this.onExitToHomeAction = action
    }

    fun updateCount(count: Int, limit: Int) {
        mainHandler.post {
            if (!Settings.canDrawOverlays(context)) return@post

            ensurePillOverlayCreated()
            pillView?.visibility = View.VISIBLE

            val brainState = BrainState.fromScrollCount(count, limit)
            val accentColor = brainState.accentColorHex.toInt()

            countNumberView?.text = count.toString()
            limitBadgeView?.text = "/$limit"

            iconView?.setImageDrawable(ContextCompat.getDrawable(context, brainState.vectorResId))
            pillBackground?.setStroke(dpToPx(1.5f), accentColor)
            limitBadgeView?.setTextColor(accentColor)

            // Trigger animations on scroll increments
            if (count != lastCount && lastCount != -1) {
                animateCounterPulse()

                // Welding sparks inside pill in FATIGUE state
                if (brainState == BrainState.FATIGUE) {
                    particleEmitter?.emitSparks(dpToPx(16f).toFloat(), dpToPx(14f).toFloat(), count = 8)
                }

                // Temporary non-blocking psychological nudge banner
                checkAndTriggerNudge(count, limit)

                // Check for Center Reality Check Intervention
                if (count >= limit && (lastInterventionDismissCount == -1 || count >= lastInterventionDismissCount + 10)) {
                    showRealityCheckIntervention(count, limit)
                }
            }

            particleEmitter?.setThermalActive(brainState == BrainState.DEPLETED)
            lastCount = count
        }
    }

    private fun checkAndTriggerNudge(count: Int, limit: Int) {
        val now = SystemClock.uptimeMillis()
        if (now - lastNudgeTime < 25_000L) return // Spacing out nudges

        val nudge = RealityNudgeRegistry.getNudge(count, limit) ?: return
        lastNudgeTime = now
        showTemporaryNudgeBanner(nudge)
    }

    private fun showTemporaryNudgeBanner(text: String) {
        if (!Settings.canDrawOverlays(context)) return
        dismissNudgeBanner()

        // Separate, standalone non-blocking window (FLAG_NOT_TOUCHABLE ensures Instagram/YouTube touches are never blocked)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(52f) // Floats comfortably below camera cutout and pill
        }

        val banner = TextView(context).apply {
            this.text = text
            textSize = 10.5f
            setTextColor(0xFFF1F5F9.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dpToPx(14f), dpToPx(5f), dpToPx(14f), dpToPx(5f))
            background = GradientDrawable().apply {
                setColor(0xF00A0E18.toInt())
                cornerRadius = dpToPx(16f).toFloat()
                setStroke(dpToPx(1f), 0x55475569.toInt())
            }
            alpha = 0f
        }

        nudgeBannerView = banner
        try {
            windowManager.addView(banner, params)
            isNudgeAttached = true

            banner.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        mainHandler.postDelayed({
                            banner.animate()
                                .alpha(0f)
                                .setDuration(350)
                                .setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(anim: Animator) {
                                        dismissNudgeBanner()
                                    }
                                })
                                .start()
                        }, 3500L)
                    }
                })
                .start()
        } catch (_: Exception) {
        }
    }

    private fun dismissNudgeBanner() {
        if (isNudgeAttached && nudgeBannerView != null) {
            try {
                windowManager.removeView(nudgeBannerView)
            } catch (_: Exception) {
            }
            nudgeBannerView = null
            isNudgeAttached = false
        }
    }

    private fun animateCounterPulse() {
        val target = countNumberView ?: return
        val anim = ValueAnimator.ofFloat(1.0f, 1.35f, 1.0f).apply {
            duration = 260
            interpolator = OvershootInterpolator(2.2f)
            addUpdateListener {
                val scale = it.animatedValue as Float
                target.scaleX = scale
                target.scaleY = scale
            }
        }
        anim.start()
    }

    fun hideOverlay() {
        mainHandler.post {
            pillView?.visibility = View.GONE
            particleEmitter?.setThermalActive(false)
            dismissNudgeBanner()
            dismissRealityCheckIntervention()
        }
    }

    fun destroy() {
        mainHandler.post {
            dismissRealityCheckIntervention()
            dismissNudgeBanner()
            if (isPillAttached && pillView != null) {
                try {
                    windowManager.removeView(pillView)
                } catch (_: Exception) {
                }
                pillView = null
                isPillAttached = false
            }
        }
    }

    // ==========================================
    // Center Reality Check Intervention Card
    // ==========================================

    @SuppressLint("ClickableViewAccessibility")
    fun showRealityCheckIntervention(count: Int, limit: Int) {
        if (isRealityCheckAttached && realityCheckView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val root = FrameLayout(context).apply {
            setBackgroundColor(0xD9060910.toInt()) // 85% dark obsidian glass scrim
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dpToPx(24f), dpToPx(26f), dpToPx(24f), dpToPx(26f))

            background = GradientDrawable().apply {
                setColor(0xFC0B0F1A.toInt())
                cornerRadius = dpToPx(24f).toFloat()
                setStroke(dpToPx(2f), 0xFFFF1744.toInt())
            }
            elevation = dpToPx(20f).toFloat()

            val cardParams = FrameLayout.LayoutParams(
                dpToPx(320f),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = cardParams
        }

        val alertBadge = TextView(context).apply {
            text = "REALITY CHECK · QUOTA BREACHED"
            textSize = 10f
            setTextColor(0xFFFF5252.toInt())
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.12f
            setPadding(dpToPx(12f), dpToPx(4f), dpToPx(12f), dpToPx(4f))
            background = GradientDrawable().apply {
                setColor(0x33FF1744.toInt())
                cornerRadius = dpToPx(12f).toFloat()
                setStroke(dpToPx(1f), 0x66FF1744.toInt())
            }
        }

        val brainIcon = ImageView(context).apply {
            val size = dpToPx(44f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                topMargin = dpToPx(14f)
                bottomMargin = dpToPx(12f)
            }
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_brain_depleted))
        }

        val title = TextView(context).apply {
            text = "Will watching further help you?"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8f)
            }
        }

        val subtitle = TextView(context).apply {
            text = "You are trading precious, irreplaceable hours of your day which will never return. Step back and break the loop."
            textSize = 12.5f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.25f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16f)
            }
        }

        val statBadge = TextView(context).apply {
            text = "$count REELS CONSUMED (LIMIT: $limit)"
            textSize = 10.5f
            setTextColor(0xFFE2E8F0.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding(dpToPx(12f), dpToPx(5f), dpToPx(12f), dpToPx(5f))
            background = GradientDrawable().apply {
                setColor(0x331E293B.toInt())
                cornerRadius = dpToPx(8f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(20f)
            }
        }

        val exitButton = TextView(context).apply {
            text = "STEP AWAY & EXIT"
            textSize = 13f
            setTextColor(0xFF03100B.toInt())
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(12f), 0, dpToPx(12f))
            background = GradientDrawable().apply {
                setColor(0xFF10B981.toInt())
                cornerRadius = dpToPx(14f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10f)
            }

            setOnClickListener {
                dismissRealityCheckIntervention()
                onExitToHomeAction?.invoke()
            }
        }

        val holdContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(42f)
            )
            background = GradientDrawable().apply {
                setColor(0x221E293B.toInt())
                cornerRadius = dpToPx(12f).toFloat()
                setStroke(dpToPx(1f), 0x4464748B.toInt())
            }
        }

        val holdProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            max = 100
            progress = 0
            progressDrawable = GradientDrawable().apply {
                setColor(0x66475569.toInt())
                cornerRadius = dpToPx(12f).toFloat()
            }
        }

        val holdLabel = TextView(context).apply {
            text = "HOLD 2S TO CONTINUE"
            textSize = 11f
            setTextColor(0xFF94A3B8.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.06f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        holdContainer.addView(holdProgressBar)
        holdContainer.addView(holdLabel)

        var holdAnimator: ValueAnimator? = null
        holdContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    holdAnimator?.cancel()
                    holdAnimator = ValueAnimator.ofInt(0, 100).apply {
                        duration = 2000L
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener {
                            holdProgressBar.progress = it.animatedValue as Int
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                if (holdProgressBar.progress >= 100) {
                                    lastInterventionDismissCount = count
                                    dismissRealityCheckIntervention()
                                }
                            }
                        })
                    }
                    holdAnimator?.start()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdAnimator?.cancel()
                    holdProgressBar.progress = 0
                    true
                }
                else -> false
            }
        }

        card.addView(alertBadge)
        card.addView(brainIcon)
        card.addView(title)
        card.addView(subtitle)
        card.addView(statBadge)
        card.addView(exitButton)
        card.addView(holdContainer)

        root.addView(card)
        realityCheckView = root

        try {
            windowManager.addView(realityCheckView, params)
            isRealityCheckAttached = true
        } catch (_: Exception) {
        }
    }

    fun dismissRealityCheckIntervention() {
        if (isRealityCheckAttached && realityCheckView != null) {
            try {
                windowManager.removeView(realityCheckView)
            } catch (_: Exception) {
            }
            realityCheckView = null
            isRealityCheckAttached = false
        }
    }

    // ==========================================
    // Top-Center Camera Pill HUD (Ultra-Compact)
    // ==========================================

    @SuppressLint("ClickableViewAccessibility")
    private fun ensurePillOverlayCreated() {
        if (isPillAttached && pillView != null) return

        // Tight Window Bounds: Only covers the compact capsule (~90x32 dp)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(12f) // Centered right by top camera notch
        }
        currentPillParams = params

        // Ultra-Compact Content Capsule
        val pillContent = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10f), 0, dpToPx(12f), 0)

            pillBackground = GradientDrawable().apply {
                setColor(0xF0080C14.toInt()) // Deep obsidian glass
                cornerRadius = dpToPx(16f).toFloat()
                setStroke(dpToPx(1.5f), 0xFF10B981.toInt())
            }
            background = pillBackground
            elevation = dpToPx(8f).toFloat()
        }

        val iconSize = dpToPx(18f)
        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = dpToPx(6f)
            }
            setImageDrawable(ContextCompat.getDrawable(context, BrainState.OPTIMAL.vectorResId))
        }

        countNumberView = TextView(context).apply {
            text = "0"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        }

        limitBadgeView = TextView(context).apply {
            text = "/50"
            textSize = 10f
            setTextColor(0xFF10B981.toInt())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.02f
            setPadding(dpToPx(2f), 0, 0, 0)
        }

        pillContent.addView(iconView)
        pillContent.addView(countNumberView)
        pillContent.addView(limitBadgeView)

        // Strict Capsule Root Frame: Explicitly measured to pillContent (never expands to screen)
        val capsuleRoot = object : FrameLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val h = dpToPx(32f)
                pillContent.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
                )
                val w = pillContent.measuredWidth
                setMeasuredDimension(w, h)

                val exactW = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
                val exactH = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
                particleEmitter?.measure(exactW, exactH)
            }

            override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                val w = right - left
                val h = bottom - top
                pillContent.layout(0, 0, w, h)
                particleEmitter?.layout(0, 0, w, h)
            }
        }.apply {
            clipChildren = false
            clipToPadding = false
        }

        capsuleRoot.addView(pillContent)

        // Embedded particle canvas (matched exactly to capsule bounds)
        particleEmitter = ParticleEmitterView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        capsuleRoot.addView(particleEmitter)

        // Drag handling strictly within the capsule
        capsuleRoot.setOnTouchListener { _, event ->
            val p = currentPillParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 8 || Math.abs(deltaY) > 8) {
                        isDragging = true
                    }
                    p.x = initialX + deltaX
                    p.y = initialY + deltaY
                    try {
                        windowManager.updateViewLayout(capsuleRoot, p)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        animateCounterPulse()
                    }
                    true
                }
                else -> false
            }
        }

        pillView = capsuleRoot
        try {
            windowManager.addView(pillView, params)
            isPillAttached = true
        } catch (_: Exception) {
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}
