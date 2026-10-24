package com.dalwadi.braindrain.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dalwadi.braindrain.core.ota.ApkDownloader
import com.dalwadi.braindrain.overlay.OverlayManager
import com.dalwadi.braindrain.service.BrainDrainAccessibilityService
import com.dalwadi.braindrain.ui.theme.BrainDrainTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private var isAccessibilityGranted by mutableStateOf(false)
    private var isOverlayGranted by mutableStateOf(false)

    private var previewOverlayManager: OverlayManager? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePermissionStates()

        previewOverlayManager = OverlayManager(applicationContext)

        setContent {
            BrainDrainTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    state = uiState,
                    isAccessibilityGranted = isAccessibilityGranted,
                    isOverlayGranted = isOverlayGranted,
                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    onOpenOverlaySettings = { openOverlaySettings() },
                    onPreviewOverlay = {
                        val count = if (uiState.totalCount == 0) 25 else uiState.totalCount
                        previewOverlayManager?.updateCount(count, uiState.dailyLimit)
                        handler.removeCallbacksAndMessages(null)
                        handler.postDelayed({
                            previewOverlayManager?.hideOverlay()
                        }, 8000)
                    },
                    onPreviewRealityCheck = {
                        previewOverlayManager?.showRealityCheckIntervention(
                            count = uiState.dailyLimit + 2,
                            limit = uiState.dailyLimit
                        )
                    },
                    onUpdateDailyLimit = { viewModel.setDailyLimit(it) },
                    onToggleOverlay = { viewModel.setOverlayEnabled(it) },
                    onToggleHaptic = { viewModel.setHapticEnabled(it) },
                    onResetCounters = { viewModel.resetTodayCounters() },
                    onSelectChannel = { viewModel.setOtaChannel(it) },
                    onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                    onReportIssue = { openGitHubIssues() },
                    onInstallUpdate = { info ->
                        ApkDownloader.downloadAndInstall(
                            context = this,
                            url = info.apkUrl,
                            fileName = "BrainDrain-${info.versionName}.apk"
                        )
                        viewModel.dismissOtaUpdate()
                    },
                    onDismissUpdate = { viewModel.dismissOtaUpdate() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    override fun onStop() {
        super.onStop()
        previewOverlayManager?.hideOverlay()
    }

    private fun updatePermissionStates() {
        isAccessibilityGranted = isAccessibilityServiceEnabled(this, BrainDrainAccessibilityService::class.java)
        isOverlayGranted = Settings.canDrawOverlays(this)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openGitHubIssues() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/sohamdalwadi/BrainDrain/issues/new")
        )
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${serviceClass.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any { it.equals(expectedComponentName, ignoreCase = true) }
    }
}
