package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeMode
import com.example.data.model.CameraLensOption
import com.example.data.model.MirrorMode
import com.example.data.model.PrompterAlignment
import com.example.data.model.RemoteAction
import com.example.data.model.RemoteKeyBindings
import com.example.data.model.ScrollDirection
import com.example.data.model.VideoFpsOption
import com.example.data.model.VideoResolutionOption
import com.example.ui.components.ColorSwatchRow
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.LabeledSlider
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PrompterColorPresets
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.RemoteKeyBus
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var capturingAction by remember { mutableStateOf<RemoteAction?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Live hardware key capture when customizing a Bluetooth / keyboard shortcut
    DisposableEffect(capturingAction) {
        RemoteKeyBus.isCapturingShortcut.set(capturingAction != null)
        onDispose { RemoteKeyBus.isCapturingShortcut.set(false) }
    }

    LaunchedEffect(capturingAction) {
        val action = capturingAction ?: return@LaunchedEffect
        RemoteKeyBus.events.collectLatest { keyCode ->
            mainViewModel.setKeyBinding(action, keyCode)
            capturingAction = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "All preferences are stored locally via DataStore",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Appearance & Display
        item(key = "section_appearance") {
            SettingsCard(title = "Appearance & Display") {
                Text("App Theme", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { mainViewModel.setThemeMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Prevent Screen Timeout", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Keep screen awake while reading or recording",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.keepScreenOn,
                        onCheckedChange = mainViewModel::setKeepScreenOn,
                        modifier = Modifier.testTag("settings_keep_screen_on_switch")
                    )
                }
            }
        }

        // 2. Teleprompter Defaults
        item(key = "section_prompter_defaults") {
            SettingsCard(title = "Teleprompter Defaults") {
                LabeledSlider(
                    label = "Default Scrolling Speed",
                    valueLabel = "${settings.defaultScrollSpeed} / 300",
                    value = settings.defaultScrollSpeed.toFloat(),
                    valueRange = 1f..300f,
                    onValueChange = { mainViewModel.setDefaultSpeed(it.roundToInt()) }
                )

                LabeledSlider(
                    label = "Default Font Size",
                    valueLabel = "${settings.defaultFontSizeSp.roundToInt()} sp",
                    value = settings.defaultFontSizeSp,
                    valueRange = 16f..72f,
                    onValueChange = { mainViewModel.setDefaultFontSize(it.roundToInt().toFloat()) }
                )

                LabeledSlider(
                    label = "Default Line Spacing",
                    valueLabel = String.format("%.2fx", settings.defaultLineSpacing),
                    value = settings.defaultLineSpacing,
                    valueRange = 1.0f..2.5f,
                    onValueChange = { mainViewModel.setDefaultLineSpacing(it) }
                )

                LabeledSlider(
                    label = "Default Paragraph Spacing",
                    valueLabel = "${settings.defaultParagraphSpacingDp} dp",
                    value = settings.defaultParagraphSpacingDp.toFloat(),
                    valueRange = 4f..56f,
                    onValueChange = { mainViewModel.setDefaultParagraphSpacing(it.roundToInt()) }
                )

                LabeledSlider(
                    label = "Default Side Margins",
                    valueLabel = "${settings.defaultHorizontalMarginDp} dp",
                    value = settings.defaultHorizontalMarginDp.toFloat(),
                    valueRange = 0f..96f,
                    onValueChange = { mainViewModel.setDefaultMargin(it.roundToInt()) }
                )

                LabeledSlider(
                    label = "Default Text Width",
                    valueLabel = "${(settings.defaultReadingAreaWidthFraction * 100).roundToInt()}%",
                    value = settings.defaultReadingAreaWidthFraction,
                    valueRange = 0.5f..1.0f,
                    onValueChange = { mainViewModel.setDefaultWidthFraction(it) }
                )

                Text("Default Alignment", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrompterAlignment.entries.forEach { align ->
                        FilterChip(
                            selected = settings.defaultAlignment == align,
                            onClick = { mainViewModel.setDefaultAlignment(align) },
                            label = { Text(align.label) }
                        )
                    }
                }

                Text("Default Mirror / Reading Mode", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MirrorMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.defaultMirrorMode == mode,
                            onClick = { mainViewModel.setDefaultMirrorMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Text("Default Scroll Direction", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScrollDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = settings.defaultScrollDirection == dir,
                            onClick = { mainViewModel.setDefaultScrollDirection(dir) },
                            label = { Text(dir.label) }
                        )
                    }
                }

                Text("Default Text Color", style = MaterialTheme.typography.titleSmall)
                ColorSwatchRow(
                    swatches = PrompterColorPresets.textSwatches,
                    selectedColor = Color(settings.defaultTextColorHex),
                    onSelect = mainViewModel::setDefaultTextColor
                )

                Text("Default Background Color", style = MaterialTheme.typography.titleSmall)
                ColorSwatchRow(
                    swatches = PrompterColorPresets.backgroundSwatches,
                    selectedColor = Color(settings.defaultBackgroundColorHex),
                    onSelect = mainViewModel::setDefaultBackgroundColor
                )
            }
        }

        // 3. Camera & Countdown Defaults
        item(key = "section_camera") {
            SettingsCard(title = "Camera & Countdown") {
                Text("Default Camera", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CameraLensOption.entries.forEach { lens ->
                        FilterChip(
                            selected = settings.defaultCameraLens == lens,
                            onClick = { mainViewModel.setDefaultCameraLens(lens) },
                            label = { Text(lens.label) }
                        )
                    }
                }

                Text("Default Video Resolution", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VideoResolutionOption.entries.forEach { res ->
                        FilterChip(
                            selected = settings.defaultVideoResolution == res,
                            onClick = { mainViewModel.setDefaultVideoResolution(res) },
                            label = { Text(res.label) }
                        )
                    }
                }

                Text("Default Frame Rate (FPS)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoFpsOption.entries.forEach { fps ->
                        FilterChip(
                            selected = settings.defaultVideoFps == fps,
                            onClick = { mainViewModel.setDefaultVideoFps(fps) },
                            label = { Text(fps.label) }
                        )
                    }
                }

                Text("Countdown Timer", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 3, 5, 10).forEach { sec ->
                        FilterChip(
                            selected = settings.countdownSeconds == sec,
                            onClick = { mainViewModel.setCountdownSeconds(sec) },
                            label = { Text(if (sec == 0) "Off" else "${sec}s") }
                        )
                    }
                }

                LabeledSlider(
                    label = "Camera Overlay Opacity",
                    valueLabel = "${(settings.cameraOverlayOpacity * 100).roundToInt()}%",
                    value = settings.cameraOverlayOpacity,
                    valueRange = 0.15f..0.95f,
                    onValueChange = mainViewModel::setCameraOverlayOpacity
                )
            }
        }

        // 4. Bluetooth & External Remote Control
        item(key = "section_remote") {
            SettingsCard(title = "Bluetooth Remote & Keyboard Shortcuts") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Works with Bluetooth keyboards, presentation clickers, and external HID remotes. Tap any shortcut to remap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Volume Button Speed Control", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Use hardware Volume Up / Down to adjust speed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.remoteBindings.volumeButtonsEnabled,
                        onCheckedChange = mainViewModel::setVolumeButtonsEnabled,
                        modifier = Modifier.testTag("settings_volume_buttons_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                RemoteAction.entries.forEach { action ->
                    val currentKey = settings.remoteBindings.keyCodeFor(action)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { capturingAction = action }
                            .padding(vertical = 8.dp)
                            .testTag("settings_bind_${action.name.lowercase()}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = RemoteKeyBindings.keyCodeDisplayName(currentKey),
                                fontFamily = MonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                TextButton(
                    onClick = mainViewModel::resetRemoteBindings,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Restore Default Shortcuts")
                }
            }
        }

        // 5. Privacy Architecture & Reset
        item(key = "section_privacy_reset") {
            SettingsCard(title = "Offline Privacy & Reset") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Smart Teleprompter Pro contains zero AI models, zero cloud SDKs, zero analytics, and does not request the Android INTERNET permission. All scripts, preferences, and videos remain strictly on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_reset_all_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset All Settings to Defaults")
                }
            }
        }

        item(key = "bottom_pad") {
            Spacer(Modifier.height(20.dp))
        }
    }

    // Key Capture Modal
    capturingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { capturingAction = null },
            title = { Text("Assign Key: ${action.label}") },
            text = {
                Text(
                    text = "Press any key on your connected Bluetooth remote, presentation clicker, or external keyboard now to bind it to \"${action.label}\".",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { capturingAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Reset Settings?",
            message = "Restore all teleprompter, camera, appearance, and shortcut settings to factory defaults? Your saved scripts and videos will not be deleted.",
            confirmLabel = "Reset",
            onConfirm = {
                mainViewModel.resetAllSettings()
                showResetConfirmDialog = false
            },
            onDismiss = { showResetConfirmDialog = false }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
