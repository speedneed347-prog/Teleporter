package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Broadcast Studio Obsidian & Amber Palette
val ObsidianCanvas = Color(0xFF0C0E13)
val StudioSurface = Color(0xFF141821)
val StudioSurfaceElevated = Color(0xFF1C2230)
val StudioSurfaceHighest = Color(0xFF252D3F)
val StudioOutline = Color(0xFF2E384D)
val StudioOutlineSubtle = Color(0xFF1F2636)

val BroadcastAmber = Color(0xFFF59E0B)
val BroadcastAmberLight = Color(0xFFFCD34D)
val BroadcastAmberDark = Color(0xFFB45309)
val BroadcastAmberContainer = Color(0xFF332208)

val StudioTeal = Color(0xFF14B8A6)
val StudioTealContainer = Color(0xFF0A2E2B)

val TallyCrimson = Color(0xFFEF4444)
val TallyCrimsonContainer = Color(0xFF3B1114)

val InkPrimaryDark = Color(0xFFF4F6FA)
val InkSecondaryDark = Color(0xFFA3B0C6)

// Daylight Studio Light Palette
val DaylightCanvas = Color(0xFFF6F7FA)
val DaylightSurface = Color(0xFFFFFFFF)
val DaylightSurfaceElevated = Color(0xFFECEFF5)
val DaylightOutline = Color(0xFFD0D7E3)

val AmberLightMode = Color(0xFFD97706)
val AmberLightContainer = Color(0xFFFEF3C7)
val TealLightMode = Color(0xFF0D9488)
val TealLightContainer = Color(0xFFCCFBF1)
val CrimsonLightMode = Color(0xFFDC2626)
val CrimsonLightContainer = Color(0xFFFEE2E2)

val InkPrimaryLight = Color(0xFF111827)
val InkSecondaryLight = Color(0xFF4B5563)

// Teleprompter Preset Swatches (ARGB Long)
object PrompterColorPresets {
    val backgroundSwatches: List<Pair<String, Long>> = listOf(
        "True Black" to 0xFF000000L,
        "Obsidian" to 0xFF0C0E13L,
        "Studio Navy" to 0xFF0B132BL,
        "Deep Forest" to 0xFF0A1F18L,
        "Warm Paper" to 0xFFFAF6EEL,
        "Pure White" to 0xFFFFFFFFL,
        "Chroma Green" to 0xFF00B140L
    )

    val textSwatches: List<Pair<String, Long>> = listOf(
        "Pure White" to 0xFFFFFFFFL,
        "Warm Ivory" to 0xFFF8F5ECL,
        "Broadcast Amber" to 0xFFFCD34DL,
        "Studio Cyan" to 0xFF5EEAD4L,
        "Signal Green" to 0xFF86EFACL,
        "Obsidian Ink" to 0xFF111827L,
        "Deep Charcoal" to 0xFF1F2937L
    )
}
