package com.dalwadi.braindrain.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.dalwadi.braindrain.core.data.PreferencesRepository
import com.dalwadi.braindrain.core.detector.DetectorRegistry
import com.dalwadi.braindrain.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BrainDrainAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingHideRunnable = Runnable { overlayManager.hideOverlay() }

    private lateinit var repository: PreferencesRepository
    private lateinit var overlayManager: OverlayManager
    private var vibrator: Vibrator? = null

    // System packages that should never trigger hiding the floater
    private val ignoredSystemPackages = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.google.android.gms",
        "com.motorola.systemui",
        "com.motorola.gestures",
        "com.motorola.launcher3"
    )

    override fun onCreate() {
        super.onCreate()
        repository = PreferencesRepository(applicationContext)
        overlayManager = OverlayManager(applicationContext)
        overlayManager.setOnExitToHomeListener {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Ignore background system events so they don't cause overlay flickering
        if (packageName in ignoredSystemPackages) {
            if (packageName == "com.motorola.launcher3" && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // User went back to home screen launcher: schedule graceful hide
                scheduleGracefulHide()
            }
            return
        }

        // 2. If the user transitioned to another third-party app, schedule graceful hide
        if (!DetectorRegistry.isSupportedPackage(packageName)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                scheduleGracefulHide()
            }
            return
        }

        // 3. User is actively in a supported target app (Instagram, YouTube)
        // Immediately cancel any pending hide to prevent flickering
        cancelPendingHide()

        val detector = DetectorRegistry.getDetector(packageName) ?: return

        serviceScope.launch {
            val isOverlayEnabled = repository.isOverlayEnabledFlow.first()
            if (!isOverlayEnabled) return@launch

            val dailyLimit = repository.dailyLimitFlow.first()
            val currentInsta = repository.dailyInstagramCountFlow.first()
            val currentYt = repository.dailyYouTubeCountFlow.first()
            val totalSoFar = currentInsta + currentYt

            // Keep pill stably visible
            overlayManager.updateCount(totalSoFar, dailyLimit)

            // Check vertical scroll / swipe
            if (detector.isForwardScrollEvent(event)) {
                val updatedTotal = repository.incrementCount(packageName)
                overlayManager.updateCount(updatedTotal, dailyLimit)

                val isHaptic = repository.isHapticEnabledFlow.first()
                if (isHaptic) {
                    triggerHapticTick(updatedTotal >= dailyLimit)
                }
            }
        }
    }

    private fun scheduleGracefulHide() {
        mainHandler.removeCallbacks(pendingHideRunnable)
        mainHandler.postDelayed(pendingHideRunnable, 1200L) // Grace period prevents flickering
    }

    private fun cancelPendingHide() {
        mainHandler.removeCallbacks(pendingHideRunnable)
    }

    private fun triggerHapticTick(isCritical: Boolean) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = if (isCritical) 140L else 35L
            val amplitude = if (isCritical) VibrationEffect.DEFAULT_AMPLITUDE else 85
            v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(if (isCritical) 140L else 35L)
        }
    }

    override fun onInterrupt() {
        overlayManager.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPendingHide()
        overlayManager.destroy()
        serviceScope.cancel()
    }
}
