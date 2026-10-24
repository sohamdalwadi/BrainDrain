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
import com.dalwadi.braindrain.core.model.BrainState
import com.dalwadi.braindrain.core.model.RealityNudgeRegistry
import com.dalwadi.braindrain.R

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    // 1. Floating Pill HUD Elements
    private var pillRootView: View? = null
    private var isPillAttached = false
    private var particleEmitter: ParticleEmitterView? = null
    private var iconView: ImageView? = null
    private var countNumberView: TextView? = null
    private var countLabelView: TextView? = null
    private var limitBadgeView: TextView? = null
    private var nudgeTickerView: TextView? = null
    private var pillBackground: GradientDrawable? = null
    private var currentPillParams: WindowManager.LayoutParams? = null

    // Pill dragging state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var lastCount = -1
    private var lastNudgeTime = 0L

    // 2. Center Reality Check Intervention Card Elements
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
            pillRootView?.visibility = View.VISIBLE

            val brainState = BrainState.fromScrollCount(count, limit)
            val accentColor = brainState.accentColorHex.toInt()

            countNumberView?.text = count.toString()
            countLabelView?.text = if (count == 1) "REEL" else "REELS"
            limitBadgeView?.text = "/ $limit"

            iconView?.setImageDrawable(ContextCompat.getDrawable(context, brainState.vectorResId))
            pillBackground?.setStroke(dpToPx(1.5f), accentColor)
            limitBadgeView?.setTextColor(accentColor)

            // Trigger animations on scroll increments
            if (count != lastCount && lastCount != -1) {
                animateCounterPulse()

                // Welding sparks animation in FATIGUE state
                if (brainState == BrainState.FATIGUE) {
                    val iconX = dpToPx(25f).toFloat()
                    val iconY = dpToPx(20f).toFloat()
                    particleEmitter?.emitSparks(iconX, iconY, count = 10)
                }

                // Check for witty/stoic psychological reality nudge
                checkAndTriggerNudge(count, limit)

                // Check for Center Reality Check Intervention
                if (count >= limit && (lastInterventionDismissCount == -1 || count >= lastInterventionDismissCount + 10)) {
                    showRealityCheckIntervention(count, limit)
                }
            }

            // Thermal melting / ember animation in DEPLETED state
            particleEmitter?.setThermalActive(brainState == BrainState.DEPLETED)

            lastCount = count
        }
    }

    private fun checkAndTriggerNudge(count: Int, limit: Int) {
        val now = SystemClock.uptimeMillis()
        if (now - lastNudgeTime < 15_000L) return // At least 15s between nudges

        val nudge = RealityNudgeRegistry.getNudge(count, limit) ?: return
        lastNudgeTime = now
        displayNudgeMarquee(nudge)
    }

    private fun displayNudgeMarquee(text: String) {
        val ticker = nudgeTickerView ?: return
        ticker.text = text
        ticker.alpha = 0f
        ticker.visibility = View.VISIBLE

        ticker.animate()
            .alpha(1f)
            .setDuration(350)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mainHandler.postDelayed({
                        ticker.animate()
                            .alpha(0f)
                            .setDuration(400)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(anim: Animator) {
                                    ticker.visibility = View.GONE
                                }
                            })
                            .start()
                    }, 4000L) // Show for 4 seconds
                }
            })
            .start()
    }

    private fun animateCounterPulse() {
        val target = countNumberView ?: return
        val anim = ValueAnimator.ofFloat(1.0f, 1.35f, 1.0f).apply {
            duration = 300
            interpolator = OvershootInterpolator(2.5f)
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
            pillRootView?.visibility = View.GONE
            particleEmitter?.setThermalActive(false)
        }
    }

    fun destroy() {
        mainHandler.post {
            dismissRealityCheckIntervention()
            if (isPillAttached && pillRootView != null) {
                try {
                    windowManager.removeView(pillRootView)
                } catch (_: Exception) {
                }
                pillRootView = null
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
            setPadding(dpToPx(24f), dpToPx(28f), dpToPx(24f), dpToPx(28f))

            background = GradientDrawable().apply {
                setColor(0xFC0B0F1A.toInt()) // Deep midnight card
                cornerRadius = dpToPx(24f).toFloat()
                setStroke(dpToPx(2f), 0xFFFF1744.toInt()) // Crimson alert border
            }
            elevation = dpToPx(20f).toFloat()

            val cardParams = FrameLayout.LayoutParams(
                dpToPx(340f),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = cardParams
        }

        // Header Alert Pill
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

        // Vector Depleted Brain Icon
        val brainIcon = ImageView(context).apply {
            val size = dpToPx(48f)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                topMargin = dpToPx(16f)
                bottomMargin = dpToPx(14f)
            }
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_brain_depleted))
        }

        // Main Intervention Heading
        val title = TextView(context).apply {
            text = "Will watching further help you?"
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10f)
            }
        }

        // Subtitle Reality Message
        val subtitle = TextView(context).apply {
            text = "You are trading precious, irreplaceable hours of your day which will never return. Step back and break the loop."
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.25f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(18f)
            }
        }

        // Stats pill
        val statBadge = TextView(context).apply {
            text = "$count REELS CONSUMED (LIMIT: $limit)"
            textSize = 11f
            setTextColor(0xFFE2E8F0.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding(dpToPx(14f), dpToPx(6f), dpToPx(14f), dpToPx(6f))
            background = GradientDrawable().apply {
                setColor(0x331E293B.toInt())
                cornerRadius = dpToPx(8f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(22f)
            }
        }

        // Primary Action: STEP AWAY & EXIT
        val exitButton = TextView(context).apply {
            text = "STEP AWAY & EXIT"
            textSize = 13f
            setTextColor(0xFF03100B.toInt())
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(13f), 0, dpToPx(13f))
            background = GradientDrawable().apply {
                setColor(0xFF10B981.toInt()) // Emerald Green
                cornerRadius = dpToPx(14f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12f)
            }

            setOnClickListener {
                dismissRealityCheckIntervention()
                onExitToHomeAction?.invoke()
            }
        }

        // Secondary Action: HOLD TO DISMISS (with hold-to-continue friction progress bar)
        val holdContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44f)
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

        // Hold-to-dismiss gesture handling
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

    private fun dismissRealityCheckIntervention() {
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
    // Top-Center Camera Pill HUD Construction
    // ==========================================

    @SuppressLint("ClickableViewAccessibility")
    private fun ensurePillOverlayCreated() {
        if (isPillAttached && pillRootView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(16f) // Centered right by top camera
        }
        currentPillParams = params

        // Outer wrapper containing pill and particle emitter
        val outerContainer = FrameLayout(context).apply {
            setPadding(dpToPx(16f), dpToPx(10f), dpToPx(16f), dpToPx(10f))
            clipChildren = false
            clipToPadding = false
        }

        particleEmitter = ParticleEmitterView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        outerContainer.addView(particleEmitter)

        val columnWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Pill Capsule
        val pillCapsule = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14f), dpToPx(6f), dpToPx(16f), dpToPx(6f))

            pillBackground = GradientDrawable().apply {
                setColor(0xF0080C14.toInt()) // Deep obsidian glass surface
                cornerRadius = dpToPx(40f).toFloat()
                setStroke(dpToPx(1.5f), 0xFF10B981.toInt()) // Vibrant Emerald accent
            }
            background = pillBackground
            elevation = dpToPx(14f).toFloat()
        }

        val iconSize = dpToPx(22f)
        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = dpToPx(8f)
            }
            setImageDrawable(ContextCompat.getDrawable(context, BrainState.OPTIMAL.vectorResId))
        }

        val counterBox = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        countNumberView = TextView(context).apply {
            text = "0"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        }

        countLabelView = TextView(context).apply {
            text = "REELS"
            textSize = 10f
            setTextColor(0xFF94A3B8.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.08f
            setPadding(dpToPx(5f), 0, dpToPx(4f), 0)
        }

        limitBadgeView = TextView(context).apply {
            text = "/ 50"
            textSize = 10f
            setTextColor(0xFF10B981.toInt())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.04f
        }

        counterBox.addView(countNumberView)
        counterBox.addView(countLabelView)
        counterBox.addView(limitBadgeView)

        pillCapsule.addView(iconView)
        pillCapsule.addView(counterBox)

        // Expandable Nudge Marquee Ticker
        nudgeTickerView = TextView(context).apply {
            textSize = 10.5f
            setTextColor(0xFFF1F5F9.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dpToPx(14f), dpToPx(5f), dpToPx(14f), dpToPx(5f))
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(0xEE0F172A.toInt())
                cornerRadius = dpToPx(16f).toFloat()
                setStroke(dpToPx(1f), 0x55475569.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(260f),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6f)
            }
        }

        columnWrapper.addView(pillCapsule)
        columnWrapper.addView(nudgeTickerView)

        outerContainer.addView(columnWrapper)

        // Drag listener on the capsule
        pillCapsule.setOnTouchListener { _, event ->
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
                        windowManager.updateViewLayout(outerContainer, p)
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

        pillRootView = outerContainer
        try {
            windowManager.addView(pillRootView, params)
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
