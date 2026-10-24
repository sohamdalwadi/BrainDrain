package com.dalwadi.braindrain.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dalwadi.braindrain.BrainDrainApp
import com.dalwadi.braindrain.core.model.BrainState
import com.dalwadi.braindrain.core.ota.OtaChannel
import com.dalwadi.braindrain.core.ota.OtaRepository
import com.dalwadi.braindrain.core.ota.OtaUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class PrefsSnapshot(
    val limit: Int,
    val insta: Int,
    val yt: Int,
    val overlay: Boolean,
    val haptic: Boolean
)

private data class OtaSnapshot(
    val channel: OtaChannel,
    val otaUpdate: OtaUpdateInfo?,
    val isChecking: Boolean
)

data class DashboardUiState(
    val dailyLimit: Int = 50,
    val instagramCount: Int = 0,
    val youtubeCount: Int = 0,
    val totalCount: Int = 0,
    val brainState: BrainState = BrainState.OPTIMAL,
    val progressRatio: Float = 0f,
    val isOverlayEnabled: Boolean = true,
    val isHapticEnabled: Boolean = true,
    val isAccessibilityEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val otaChannel: OtaChannel = OtaChannel.STABLE,
    val otaUpdateInfo: OtaUpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val currentVersionName: String = "1.0.0",
    val currentVersionCode: Int = 1
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BrainDrainApp).preferencesRepository
    private val otaRepository = OtaRepository(application)

    private val selectedChannelFlow = MutableStateFlow(OtaChannel.STABLE)
    private val otaUpdateInfoFlow = MutableStateFlow<OtaUpdateInfo?>(null)
    private val isCheckingUpdateFlow = MutableStateFlow(false)

    val currentVersionName = otaRepository.getCurrentVersionName()
    val currentVersionCode = otaRepository.getCurrentVersionCode()

    private val prefsFlow = combine(
        repository.dailyLimitFlow,
        repository.dailyInstagramCountFlow,
        repository.dailyYouTubeCountFlow,
        repository.isOverlayEnabledFlow,
        repository.isHapticEnabledFlow
    ) { limit, insta, yt, overlay, haptic ->
        PrefsSnapshot(limit, insta, yt, overlay, haptic)
    }

    private val otaStateFlow = combine(
        selectedChannelFlow,
        otaUpdateInfoFlow,
        isCheckingUpdateFlow
    ) { channel, otaUpdate, isChecking ->
        OtaSnapshot(channel, otaUpdate, isChecking)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        prefsFlow,
        otaStateFlow
    ) { p, o ->
        val total = p.insta + p.yt
        val state = BrainState.fromScrollCount(total, p.limit)
        val ratio = (total.toFloat() / p.limit.coerceAtLeast(1)).coerceIn(0f, 1f)
        DashboardUiState(
            dailyLimit = p.limit,
            instagramCount = p.insta,
            youtubeCount = p.yt,
            totalCount = total,
            brainState = state,
            progressRatio = ratio,
            isOverlayEnabled = p.overlay,
            isHapticEnabled = p.haptic,
            otaChannel = o.channel,
            otaUpdateInfo = o.otaUpdate,
            isCheckingUpdate = o.isChecking,
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode
        )
    )

    init {
        checkForUpdates(silent = true)
    }

    fun setDailyLimit(limit: Int) {
        viewModelScope.launch {
            repository.setDailyLimit(limit)
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOverlayEnabled(enabled)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticEnabled(enabled)
        }
    }

    fun resetTodayCounters() {
        viewModelScope.launch {
            repository.resetTodayCounters()
        }
    }

    fun setOtaChannel(channel: OtaChannel) {
        selectedChannelFlow.value = channel
        checkForUpdates(silent = false)
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) isCheckingUpdateFlow.value = true
            try {
                val update = otaRepository.checkUpdate(selectedChannelFlow.value)
                otaUpdateInfoFlow.value = update
            } finally {
                if (!silent) isCheckingUpdateFlow.value = false
            }
        }
    }

    fun dismissOtaUpdate() {
        otaUpdateInfoFlow.value = null
    }
}
