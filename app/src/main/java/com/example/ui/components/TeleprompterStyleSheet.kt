package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MirrorMode
import com.example.data.model.PrompterAlignment
import com.example.data.model.ScrollDirection
import com.example.ui.theme.PrompterColorPresets
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeleprompterStyleBottomSheet(
    style: TeleprompterStyleConfig,
    countdownSeconds: Int,
    showBackgroundColors: Boolean = true,
    cameraOverlayOpacity: Float? = null,
    onStyleChange: (TeleprompterStyleConfig) -> Unit,
    onCountdownChange: (Int) -> Unit,
    onOverlayOpacityChange: ((Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prompter Appearance & Layout",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_style_sheet_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close appearance panel")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Typography & Spacing sliders
            LabeledSlider(
                label = "Text Size",
                valueLabel = "${style.fontSizeSp.roundToInt()} sp",
                value = style.fontSizeSp,
                valueRange = 16f..72f,
                onValueChange = { onStyleChange(style.copy(fontSizeSp = it.roundToInt().toFloat())) }
            )

            LabeledSlider(
                label = "Scroll Speed",
                valueLabel = "${style.scrollSpeed} / 300",
                value = style.scrollSpeed.toFloat(),
                valueRange = 1f..300f,
                onValueChange = { onStyleChange(style.copy(scrollSpeed = it.roundToInt().coerceIn(1, 300))) }
            )

            LabeledSlider(
                label = "Line Spacing",
                valueLabel = String.format("%.2fx", style.lineSpacingMultiplier),
                value = style.lineSpacingMultiplier,
                valueRange = 1.0f..2.5f,
                onValueChange = { onStyleChange(style.copy(lineSpacingMultiplier = it)) }
            )

            LabeledSlider(
                label = "Paragraph Spacing",
                valueLabel = "${style.paragraphSpacingDp} dp",
                value = style.paragraphSpacingDp.toFloat(),
                valueRange = 4f..56f,
                onValueChange = { onStyleChange(style.copy(paragraphSpacingDp = it.roundToInt())) }
            )

            LabeledSlider(
                label = "Side Margins",
                valueLabel = "${style.horizontalMarginDp} dp",
                value = style.horizontalMarginDp.toFloat(),
                valueRange = 0f..96f,
                onValueChange = { onStyleChange(style.copy(horizontalMarginDp = it.roundToInt())) }
            )

            LabeledSlider(
                label = "Reading Width",
                valueLabel = "${(style.readingAreaWidthFraction * 100).roundToInt()}%",
                value = style.readingAreaWidthFraction,
                valueRange = 0.5f..1.0f,
                onValueChange = { onStyleChange(style.copy(readingAreaWidthFraction = it)) }
            )

            if (cameraOverlayOpacity != null && onOverlayOpacityChange != null) {
                LabeledSlider(
                    label = "Overlay Backdrop Opacity",
                    valueLabel = "${(cameraOverlayOpacity * 100).roundToInt()}%",
                    value = cameraOverlayOpacity,
                    valueRange = 0.15f..0.95f,
                    onValueChange = onOverlayOpacityChange
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Alignment
            Text("Text Alignment", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrompterAlignment.entries.forEach { align ->
                    FilterChip(
                        selected = style.alignment == align,
                        onClick = { onStyleChange(style.copy(alignment = align)) },
                        label = { Text(align.label) }
                    )
                }
            }

            // Mirror Mode
            Text("Mirror Mode (Rig Reflection)", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MirrorMode.entries.forEach { mode ->
                    FilterChip(
                        selected = style.mirrorMode == mode,
                        onClick = { onStyleChange(style.copy(mirrorMode = mode)) },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("mirror_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Scroll Direction
            Text("Scroll Direction", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrollDirection.entries.forEach { dir ->
                    FilterChip(
                        selected = style.scrollDirection == dir,
                        onClick = { onStyleChange(style.copy(scrollDirection = dir)) },
                        label = { Text(dir.label) }
                    )
                }
            }

            // Countdown
            Text("Start Countdown", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 5, 10).forEach { sec ->
                    FilterChip(
                        selected = countdownSeconds == sec,
                        onClick = { onCountdownChange(sec) },
                        label = { Text(if (sec == 0) "Off" else "${sec}s") }
                    )
                }
            }

            // Text Colors
            Text("Text Color", style = MaterialTheme.typography.titleSmall)
            ColorSwatchRow(
                swatches = PrompterColorPresets.textSwatches,
                selectedColor = style.textColor,
                onSelect = { colorLong -> onStyleChange(style.copy(textColor = Color(colorLong))) }
            )

            // Background Colors
            if (showBackgroundColors) {
                Text("Background Color", style = MaterialTheme.typography.titleSmall)
                ColorSwatchRow(
                    swatches = PrompterColorPresets.backgroundSwatches,
                    selectedColor = style.backgroundColor,
                    onSelect = { colorLong -> onStyleChange(style.copy(backgroundColor = Color(colorLong))) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchRow(
    swatches: List<Pair<String, Long>>,
    selectedColor: Color,
    onSelect: (Long) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        swatches.forEach { (name, hex) ->
            val swatchColor = Color(hex)
            val isSelected = selectedColor.value == swatchColor.value
            val checkTint = if (hex == 0xFFFFFFFFL || hex == 0xFFFAF6EEL || hex == 0xFFF8F5ECL || hex == 0xFFFCD34DL) {
                Color.Black
            } else {
                Color.White
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(
                        width = if (isSelected) 3.dp else 1.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "$name selected",
                        tint = checkTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
