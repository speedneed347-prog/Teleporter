package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AppThemeMode

private val StudioDarkColorScheme = darkColorScheme(
    primary = BroadcastAmber,
    onPrimary = Color(0xFF1A1100),
    primaryContainer = BroadcastAmberContainer,
    onPrimaryContainer = BroadcastAmberLight,
    secondary = StudioTeal,
    onSecondary = Color(0xFF001F1C),
    secondaryContainer = StudioTealContainer,
    onSecondaryContainer = Color(0xFF5EEAD4),
    tertiary = TallyCrimson,
    onTertiary = Color.White,
    tertiaryContainer = TallyCrimsonContainer,
    onTertiaryContainer = Color(0xFFFCA5A5),
    background = ObsidianCanvas,
    onBackground = InkPrimaryDark,
    surface = StudioSurface,
    onSurface = InkPrimaryDark,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = InkSecondaryDark,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = StudioOutline,
    outlineVariant = StudioOutlineSubtle,
    error = TallyCrimson,
    onError = Color.White,
    errorContainer = TallyCrimsonContainer,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val StudioLightColorScheme = lightColorScheme(
    primary = AmberLightMode,
    onPrimary = Color.White,
    primaryContainer = AmberLightContainer,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = TealLightMode,
    onSecondary = Color.White,
    secondaryContainer = TealLightContainer,
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = CrimsonLightMode,
    onTertiary = Color.White,
    tertiaryContainer = CrimsonLightContainer,
    onTertiaryContainer = Color(0xFF991B1B),
    background = DaylightCanvas,
    onBackground = InkPrimaryLight,
    surface = DaylightSurface,
    onSurface = InkPrimaryLight,
    surfaceVariant = DaylightSurfaceElevated,
    onSurfaceVariant = InkSecondaryLight,
    surfaceContainerLow = DaylightSurface,
    surfaceContainer = DaylightSurfaceElevated,
    surfaceContainerHigh = Color(0xFFE2E7F0),
    outline = DaylightOutline,
    outlineVariant = Color(0xFFE5E7EB),
    error = CrimsonLightMode,
    onError = Color.White,
    errorContainer = CrimsonLightContainer,
    onErrorContainer = Color(0xFF991B1B)
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartTeleprompterTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDark) StudioDarkColorScheme else StudioLightColorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
