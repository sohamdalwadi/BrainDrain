package com.dalwadi.braindrain.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dalwadi.braindrain.R
import com.dalwadi.braindrain.core.ota.OtaChannel
import com.dalwadi.braindrain.core.ota.OtaUpdateInfo
import com.dalwadi.braindrain.ui.theme.AccentEmerald
import com.dalwadi.braindrain.ui.theme.DarkBackground
import com.dalwadi.braindrain.ui.theme.DarkBorder
import com.dalwadi.braindrain.ui.theme.DarkSurface
import com.dalwadi.braindrain.ui.theme.DarkSurfaceElevated
import com.dalwadi.braindrain.ui.theme.TextMuted
import com.dalwadi.braindrain.ui.theme.TextPrimary
import com.dalwadi.braindrain.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onPreviewOverlay: () -> Unit,
    onPreviewRealityCheck: () -> Unit = {},
    onUpdateDailyLimit: (Int) -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onResetCounters: () -> Unit,
    onSelectChannel: (OtaChannel) -> Unit,
    onCheckForUpdates: () -> Unit,
    onReportIssue: () -> Unit,
    onInstallUpdate: (OtaUpdateInfo) -> Unit,
    onDismissUpdate: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val animatedProgress by animateFloatAsState(
        targetValue = state.progressRatio,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )
    val stateColor = Color(state.brainState.accentColorHex)
    val animatedStateColor by animateColorAsState(
        targetValue = stateColor,
        animationSpec = tween(durationMillis = 500),
        label = "state_color"
    )

    // OTA Update Alert Dialog (Vector Aesthetic, Zero Emojis)
    state.otaUpdateInfo?.let { updateInfo ->
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            shape = RoundedCornerShape(20.dp),
            containerColor = DarkSurface,
            tonalElevation = 8.dp,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Update Available",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "BrainDrain v${updateInfo.versionName} is ready for installation on the ${updateInfo.channel} channel.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = updateInfo.releaseNotes,
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onInstallUpdate(updateInfo) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Text("INSTALL NOW", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdate) {
                    Text("LATER", color = TextMuted, fontSize = 12.sp)
                }
            }
        )
    }

    // MAIN DASHBOARD CONTENT (Zero debug buttons, clean executive layout)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BRAINDRAIN",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Architect: Soham Dalwadi",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated)
                        .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "FOSS",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(BorderStroke(1.dp, DarkBorder), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Brain State Vector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPreviewOverlay() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.5.dp, animatedStateColor.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vector Brain Icon with Breathing Aura and Orbiting Energy Particles
                HeroBrainAura(
                    stateColor = animatedStateColor,
                    vectorResId = state.brainState.vectorResId,
                    title = state.brainState.title
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = state.brainState.title.uppercase(),
                    color = animatedStateColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.brainState.subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap card to test overlay & particle physics",
                    color = animatedStateColor.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = animatedStateColor,
                    trackColor = DarkBorder,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${state.totalCount} SCROLLS",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "CAP: ${state.dailyLimit}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Platform Breakdown
        Text(
            text = "PLATFORM BREAKDOWN",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlatformCard(
                modifier = Modifier.weight(1f),
                title = "Instagram",
                count = state.instagramCount,
                vectorResId = R.drawable.ic_instagram,
                accentColor = Color(0xFFE1306C)
            )

            PlatformCard(
                modifier = Modifier.weight(1f),
                title = "YouTube Shorts",
                count = state.youtubeCount,
                vectorResId = R.drawable.ic_youtube_shorts,
                accentColor = Color(0xFFFF0000)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick System Health Status Strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isAccessibilityGranted && isOverlayGranted) AccentEmerald else Color(0xFFFF9100))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAccessibilityGranted && isOverlayGranted) "Engine Active & Tracking" else "Setup Required",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "SETTINGS",
                    color = AccentEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.clickable { showSettingsSheet = true }
                )
            }
        }
    }

    // ==========================================
    // DEDICATED SETTINGS & DIAGNOSTICS BOTTOM SHEET
    // ==========================================
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            scrimColor = Color.Black.copy(alpha = 0.65f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DarkBorder)
                )
            }
        ) {
            val sheetScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(sheetScroll)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "SETTINGS & PREFERENCES",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 1: PREFERENCES & CAPS
                Text(
                    text = "USAGE PREFERENCES",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Daily Limit", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${state.dailyLimit} Reels",
                                color = AccentEmerald,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = state.dailyLimit.toFloat(),
                            onValueChange = { onUpdateDailyLimit(it.toInt()) },
                            valueRange = 10f..200f,
                            steps = 18,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentEmerald,
                                activeTrackColor = AccentEmerald,
                                inactiveTrackColor = DarkBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Floating Counter HUD", color = TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = state.isOverlayEnabled,
                                onCheckedChange = onToggleOverlay,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentEmerald,
                                    checkedTrackColor = AccentEmerald.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Tactile Haptic Feedback", color = TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = state.isHapticEnabled,
                                onCheckedChange = onToggleHaptic,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentEmerald,
                                    checkedTrackColor = AccentEmerald.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onResetCounters,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESET TODAY'S SESSION", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 2: SYSTEM PERMISSIONS
                Text(
                    text = "PERMISSIONS & SERVICES",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatusRow(
                            title = "Accessibility Engine",
                            description = "Required to detect reels and shorts swipe gestures",
                            isGranted = isAccessibilityGranted,
                            actionText = "ENABLE",
                            onAction = onOpenAccessibilitySettings
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusRow(
                            title = "Floating Window",
                            description = "Displays the counter over active feeds",
                            isGranted = isOverlayGranted,
                            actionText = "GRANT",
                            onAction = onOpenOverlaySettings
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 3: UPDATES & CHANNELS
                Text(
                    text = "UPDATES & REPOSITORY",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Release Channel", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OtaChannel.entries.forEach { channel ->
                                val isSelected = state.otaChannel == channel
                                Button(
                                    onClick = { onSelectChannel(channel) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) AccentEmerald else DarkSurface
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) AccentEmerald else DarkBorder)
                                ) {
                                    Text(
                                        text = channel.displayName,
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${state.currentVersionName} (${state.currentVersionCode})",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Button(
                                onClick = onCheckForUpdates,
                                enabled = !state.isCheckingUpdate,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f))
                            ) {
                                if (state.isCheckingUpdate) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentEmerald, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CHECK OTA", color = AccentEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onReportIssue,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Icon(Icons.Outlined.BugReport, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REPORT ISSUE ON GITHUB", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 4: DIAGNOSTICS & PREVIEWS (Tucked neatly at bottom of Settings)
                Text(
                    text = "DIAGNOSTICS & PREVIEWS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPreviewOverlay,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text("TEST PILL", color = AccentEmerald, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onPreviewRealityCheck,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text("TEST REALITY CHECK", color = Color(0xFFFF5252), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun PlatformCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    vectorResId: Int,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = vectorResId),
                        contentDescription = title,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = count.toString(),
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = if (isGranted) AccentEmerald else Color(0xFFFF9100),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentEmerald.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = AccentEmerald,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HeroBrainAura(
    stateColor: Color,
    vectorResId: Int,
    title: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_aura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rotation"
    )

    Box(
        modifier = Modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        // Orbiting sparks / particles canvas
        Canvas(modifier = Modifier.size(116.dp)) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val orbitRadius = size.width * 0.44f

            // Outer subtle glow ring
            drawCircle(
                color = stateColor.copy(alpha = auraAlpha * 0.4f),
                radius = orbitRadius + (pulseScale - 1f) * 20f,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Orbiting particle points
            val particleCount = 4
            val radBase = Math.toRadians(rotation.toDouble()).toFloat()
            for (i in 0 until particleCount) {
                val angle = radBase + (i * (2f * Math.PI.toFloat() / particleCount))
                val px = centerOffset.x + orbitRadius * cos(angle)
                val py = centerOffset.y + orbitRadius * sin(angle)
                val particleAlpha = ((sin(angle.toDouble()) + 1.0) / 2.0).toFloat().coerceIn(0.2f, 1f)

                drawCircle(
                    color = stateColor.copy(alpha = particleAlpha * 0.8f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // Inner glowing brain circular container
        Box(
            modifier = Modifier
                .size(92.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(stateColor.copy(alpha = auraAlpha))
                .border(BorderStroke(1.5.dp, stateColor.copy(alpha = 0.5f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = vectorResId),
                contentDescription = title,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}
