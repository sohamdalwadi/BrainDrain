package com.dalwadi.braindrain.overlay

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
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dalwadi.braindrain.core.model.BrainState

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var isViewAttached = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var iconView: ImageView? = null
    private var countNumberView: TextView? = null
    private var countLabelView: TextView? = null
    private var limitBadgeView: TextView? = null
    private var pillBackground: GradientDrawable? = null
    private var currentLayoutParams: WindowManager.LayoutParams? = null

    private var lastCount = -1

    fun updateCount(count: Int, limit: Int) {
        mainHandler.post {
            if (!Settings.canDrawOverlays(context)) return@post

            ensureOverlayCreated()

            overlayView?.visibility = View.VISIBLE

            val brainState = BrainState.fromScrollCount(count, limit)
            val accentColor = brainState.accentColorHex.toInt()

            countNumberView?.text = count.toString()
            countLabelView?.text = if (count == 1) "REEL" else "REELS"
            limitBadgeView?.text = "/ $limit"

            iconView?.setImageDrawable(ContextCompat.getDrawable(context, brainState.vectorResId))
            pillBackground?.setStroke(dpToPx(1.5f), accentColor)
            limitBadgeView?.setTextColor(accentColor)

            // Micro-pop pulse animation when count increments
            if (count != lastCount && lastCount != -1) {
                animateCounterPulse()
            }
            lastCount = count
        }
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
            // Hide without removing view to eliminate surface flicker
            overlayView?.visibility = View.GONE
        }
    }

    fun destroy() {
        mainHandler.post {
            if (isViewAttached && overlayView != null) {
                try {
                    windowManager.removeView(overlayView)
                } catch (_: Exception) {
                }
                overlayView = null
                isViewAttached = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureOverlayCreated() {
        if (isViewAttached && overlayView != null) return

        // Position: Top Center (around/below the selfie camera cutout)
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
        currentLayoutParams = params

        // Top center floating pill container
        val container = LinearLayout(context).apply {
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

        // Vector Brain state icon
        val iconSize = dpToPx(22f)
        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = dpToPx(8f)
            }
            setImageDrawable(ContextCompat.getDrawable(context, BrainState.OPTIMAL.vectorResId))
        }

        // Counter text box
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

        container.addView(iconView)
        container.addView(counterBox)

        // Draggable touch listener
        container.setOnTouchListener { _, event ->
            val p = currentLayoutParams ?: return@setOnTouchListener false
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
                        windowManager.updateViewLayout(container, p)
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

        overlayView = container
        try {
            windowManager.addView(overlayView, params)
            isViewAttached = true
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
