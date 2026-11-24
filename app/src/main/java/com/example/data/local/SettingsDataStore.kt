package com.example.data.local

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.teleprompterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_teleprompter_settings"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SPEED = intPreferencesKey("default_speed")
        val DEFAULT_FONT_SIZE = floatPreferencesKey("default_font_size")
        val DEFAULT_LINE_SPACING = floatPreferencesKey("default_line_spacing")
        val DEFAULT_PARAGRAPH_SPACING = intPreferencesKey("default_paragraph_spacing")
        val DEFAULT_MARGIN = intPreferencesKey("default_margin")
        val DEFAULT_WIDTH_FRACTION = floatPreferencesKey("default_width_fraction")
        val DEFAULT_ALIGNMENT = stringPreferencesKey("default_alignment")
        val DEFAULT_TEXT_COLOR = longPreferencesKey("default_text_color")
        val DEFAULT_BG_COLOR = longPreferencesKey("default_bg_color")
        val DEFAULT_MIRROR_MODE = stringPreferencesKey("default_mirror_mode")
        val DEFAULT_SCROLL_DIRECTION = stringPreferencesKey("default_scroll_direction")
        val DEFAULT_CAMERA_LENS = stringPreferencesKey("default_camera_lens")
        val DEFAULT_VIDEO_RESOLUTION = stringPreferencesKey("default_video_resolution")
        val DEFAULT_VIDEO_FPS = stringPreferencesKey("default_video_fps")
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val CAMERA_OVERLAY_OPACITY = floatPreferencesKey("camera_overlay_opacity")
        val KEY_PLAY_PAUSE = intPreferencesKey("key_play_pause")
        val KEY_SPEED_UP = intPreferencesKey("key_speed_up")
        val KEY_SPEED_DOWN = intPreferencesKey("key_speed_down")
        val KEY_RESTART = intPreferencesKey("key_restart")
        val KEY_EXIT = intPreferencesKey("key_exit")
        val VOLUME_BUTTONS_ENABLED = booleanPreferencesKey("volume_buttons_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.teleprompterDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
                themeMode = enumOrDefault(prefs[Keys.THEME_MODE], defaults.themeMode),
                defaultScrollSpeed = (prefs[Keys.DEFAULT_SPEED] ?: defaults.defaultScrollSpeed).coerceIn(1, 300),
                defaultFontSizeSp = (prefs[Keys.DEFAULT_FONT_SIZE] ?: defaults.defaultFontSizeSp).coerceIn(16f, 72f),
                defaultLineSpacing = (prefs[Keys.DEFAULT_LINE_SPACING] ?: defaults.defaultLineSpacing).coerceIn(1.0f, 2.5f),
                defaultParagraphSpacingDp = (prefs[Keys.DEFAULT_PARAGRAPH_SPACING] ?: defaults.defaultParagraphSpacingDp).coerceIn(4, 56),
                defaultHorizontalMarginDp = (prefs[Keys.DEFAULT_MARGIN] ?: defaults.defaultHorizontalMarginDp).coerceIn(0, 96),
                defaultReadingAreaWidthFraction = (prefs[Keys.DEFAULT_WIDTH_FRACTION] ?: defaults.defaultReadingAreaWidthFraction).coerceIn(0.5f, 1.0f),
                defaultAlignment = enumOrDefault(prefs[Keys.DEFAULT_ALIGNMENT], defaults.defaultAlignment),
                defaultTextColorHex = prefs[Keys.DEFAULT_TEXT_COLOR] ?: defaults.defaultTextColorHex,
                defaultBackgroundColorHex = prefs[Keys.DEFAULT_BG_COLOR] ?: defaults.defaultBackgroundColorHex,
                defaultMirrorMode = enumOrDefault(prefs[Keys.DEFAULT_MIRROR_MODE], defaults.defaultMirrorMode),
                defaultScrollDirection = enumOrDefault(prefs[Keys.DEFAULT_SCROLL_DIRECTION], defaults.defaultScrollDirection),
                defaultCameraLens = enumOrDefault(prefs[Keys.DEFAULT_CAMERA_LENS], defaults.defaultCameraLens),
                defaultVideoResolution = enumOrDefault(prefs[Keys.DEFAULT_VIDEO_RESOLUTION], defaults.defaultVideoResolution),
                defaultVideoFps = enumOrDefault(prefs[Keys.DEFAULT_VIDEO_FPS], defaults.defaultVideoFps),
                countdownSeconds = (prefs[Keys.COUNTDOWN_SECONDS] ?: defaults.countdownSeconds).coerceIn(0, 15),
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
                cameraOverlayOpacity = (prefs[Keys.CAMERA_OVERLAY_OPACITY] ?: defaults.cameraOverlayOpacity).coerceIn(0.15f, 0.95f),
                remoteBindings = RemoteKeyBindings(
                    playPauseKeyCode = prefs[Keys.KEY_PLAY_PAUSE] ?: KeyEvent.KEYCODE_SPACE,
                    speedUpKeyCode = prefs[Keys.KEY_SPEED_UP] ?: KeyEvent.KEYCODE_DPAD_UP,
                    speedDownKeyCode = prefs[Keys.KEY_SPEED_DOWN] ?: KeyEvent.KEYCODE_DPAD_DOWN,
                    restartKeyCode = prefs[Keys.KEY_RESTART] ?: KeyEvent.KEYCODE_R,
                    exitKeyCode = prefs[Keys.KEY_EXIT] ?: KeyEvent.KEYCODE_ESCAPE,
                    volumeButtonsEnabled = prefs[Keys.VOLUME_BUTTONS_ENABLED] ?: true
                )
            )
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.teleprompterDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.teleprompterDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateDefaultSpeed(speed: Int) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_SPEED] = speed.coerceIn(1, 300) }
    }

    suspend fun updateDefaultFontSize(sizeSp: Float) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_FONT_SIZE] = sizeSp.coerceIn(16f, 72f) }
    }

    suspend fun updateDefaultLineSpacing(multiplier: Float) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_LINE_SPACING] = multiplier.coerceIn(1.0f, 2.5f) }
    }

    suspend fun updateDefaultParagraphSpacing(dp: Int) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_PARAGRAPH_SPACING] = dp.coerceIn(4, 56) }
    }

    suspend fun updateDefaultMargin(dp: Int) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_MARGIN] = dp.coerceIn(0, 96) }
    }

    suspend fun updateDefaultWidthFraction(fraction: Float) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_WIDTH_FRACTION] = fraction.coerceIn(0.5f, 1.0f) }
    }

    suspend fun updateDefaultAlignment(alignment: PrompterAlignment) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_ALIGNMENT] = alignment.name }
    }

    suspend fun updateDefaultTextColor(colorHex: Long) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_TEXT_COLOR] = colorHex }
    }

    suspend fun updateDefaultBackgroundColor(colorHex: Long) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_BG_COLOR] = colorHex }
    }

    suspend fun updateDefaultMirrorMode(mode: MirrorMode) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_MIRROR_MODE] = mode.name }
    }

    suspend fun updateDefaultScrollDirection(direction: ScrollDirection) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_SCROLL_DIRECTION] = direction.name }
    }

    suspend fun updateDefaultCameraLens(lens: CameraLensOption) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_CAMERA_LENS] = lens.name }
    }

    suspend fun updateDefaultVideoResolution(resolution: VideoResolutionOption) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_VIDEO_RESOLUTION] = resolution.name }
    }

    suspend fun updateDefaultVideoFps(fps: VideoFpsOption) {
        context.teleprompterDataStore.edit { it[Keys.DEFAULT_VIDEO_FPS] = fps.name }
    }

    suspend fun updateCountdownSeconds(seconds: Int) {
        context.teleprompterDataStore.edit { it[Keys.COUNTDOWN_SECONDS] = seconds.coerceIn(0, 15) }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.teleprompterDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateCameraOverlayOpacity(opacity: Float) {
        context.teleprompterDataStore.edit { it[Keys.CAMERA_OVERLAY_OPACITY] = opacity.coerceIn(0.15f, 0.95f) }
    }

    suspend fun updateVolumeButtonsEnabled(enabled: Boolean) {
        context.teleprompterDataStore.edit { it[Keys.VOLUME_BUTTONS_ENABLED] = enabled }
    }

    suspend fun updateKeyBinding(action: RemoteAction, keyCode: Int) {
        context.teleprompterDataStore.edit { prefs ->
            when (action) {
                RemoteAction.PLAY_PAUSE -> prefs[Keys.KEY_PLAY_PAUSE] = keyCode
                RemoteAction.SPEED_UP -> prefs[Keys.KEY_SPEED_UP] = keyCode
                RemoteAction.SPEED_DOWN -> prefs[Keys.KEY_SPEED_DOWN] = keyCode
                RemoteAction.RESTART -> prefs[Keys.KEY_RESTART] = keyCode
                RemoteAction.EXIT_FULLSCREEN -> prefs[Keys.KEY_EXIT] = keyCode
            }
        }
    }

    suspend fun resetRemoteBindings() {
        context.teleprompterDataStore.edit { prefs ->
            prefs[Keys.KEY_PLAY_PAUSE] = KeyEvent.KEYCODE_SPACE
            prefs[Keys.KEY_SPEED_UP] = KeyEvent.KEYCODE_DPAD_UP
            prefs[Keys.KEY_SPEED_DOWN] = KeyEvent.KEYCODE_DPAD_DOWN
            prefs[Keys.KEY_RESTART] = KeyEvent.KEYCODE_R
            prefs[Keys.KEY_EXIT] = KeyEvent.KEYCODE_ESCAPE
            prefs[Keys.VOLUME_BUTTONS_ENABLED] = true
        }
    }

    suspend fun resetAllSettings() {
        context.teleprompterDataStore.edit { prefs ->
            val onboarded = prefs[Keys.ONBOARDING_COMPLETED] ?: true
            prefs.clear()
            prefs[Keys.ONBOARDING_COMPLETED] = onboarded
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
