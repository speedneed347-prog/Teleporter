package com.example.data.model

import android.view.KeyEvent

enum class AppThemeMode(val label: String) {
    DARK("Dark Studio"),
    LIGHT("Daylight"),
    SYSTEM("System")
}

enum class MirrorMode(val label: String, val shortLabel: String) {
    NORMAL("Normal", "Off"),
    HORIZONTAL("Horizontal Mirror", "Flip H"),
    VERTICAL("Vertical Mirror", "Flip V"),
    BOTH("Horizontal + Vertical", "Flip H+V")
}

enum class PrompterAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    JUSTIFIED("Justified")
}

enum class ScrollDirection(val label: String) {
    UP("Scroll Up (Standard)"),
    DOWN("Scroll Down (Reverse)")
}

enum class CameraLensOption(val label: String) {
    FRONT("Front Camera"),
    REAR("Rear Camera")
}

enum class VideoResolutionOption(val label: String, val heightPx: Int) {
    SD_480("SD 480p", 480),
    HD_720("HD 720p", 720),
    FHD_1080("Full HD 1080p", 1080),
    UHD_4K("Ultra HD 4K", 2160)
}

enum class VideoFpsOption(val label: String, val fps: Int) {
    FPS_24("24 fps", 24),
    FPS_30("30 fps", 30),
    FPS_60("60 fps", 60)
}

enum class RemoteAction(val label: String) {
    PLAY_PAUSE("Play / Pause"),
    SPEED_UP("Increase Speed"),
    SPEED_DOWN("Decrease Speed"),
    RESTART("Restart Script"),
    EXIT_FULLSCREEN("Exit Fullscreen")
}

data class RemoteKeyBindings(
    val playPauseKeyCode: Int = KeyEvent.KEYCODE_SPACE,
    val speedUpKeyCode: Int = KeyEvent.KEYCODE_DPAD_UP,
    val speedDownKeyCode: Int = KeyEvent.KEYCODE_DPAD_DOWN,
    val restartKeyCode: Int = KeyEvent.KEYCODE_R,
    val exitKeyCode: Int = KeyEvent.KEYCODE_ESCAPE,
    val volumeButtonsEnabled: Boolean = true
) {
    fun keyCodeFor(action: RemoteAction): Int = when (action) {
        RemoteAction.PLAY_PAUSE -> playPauseKeyCode
        RemoteAction.SPEED_UP -> speedUpKeyCode
        RemoteAction.SPEED_DOWN -> speedDownKeyCode
        RemoteAction.RESTART -> restartKeyCode
        RemoteAction.EXIT_FULLSCREEN -> exitKeyCode
    }

    companion object {
        fun keyCodeDisplayName(keyCode: Int): String = when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> "Space"
            KeyEvent.KEYCODE_DPAD_UP -> "Up Arrow"
            KeyEvent.KEYCODE_DPAD_DOWN -> "Down Arrow"
            KeyEvent.KEYCODE_DPAD_LEFT -> "Left Arrow"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "Right Arrow"
            KeyEvent.KEYCODE_ESCAPE -> "Escape"
            KeyEvent.KEYCODE_ENTER -> "Enter"
            KeyEvent.KEYCODE_PAGE_UP -> "Page Up"
            KeyEvent.KEYCODE_PAGE_DOWN -> "Page Down"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "Media Play/Pause"
            KeyEvent.KEYCODE_VOLUME_UP -> "Volume Up"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "Volume Down"
            else -> KeyEvent.keyCodeToString(keyCode)
                .removePrefix("KEYCODE_")
                .replace("_", " ")
        }
    }
}

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val defaultScrollSpeed: Int = 65,
    val defaultFontSizeSp: Float = 36f,
    val defaultLineSpacing: Float = 1.45f,
    val defaultParagraphSpacingDp: Int = 18,
    val defaultHorizontalMarginDp: Int = 20,
    val defaultReadingAreaWidthFraction: Float = 1.0f,
    val defaultAlignment: PrompterAlignment = PrompterAlignment.LEFT,
    val defaultTextColorHex: Long = 0xFFFFFFFFL,
    val defaultBackgroundColorHex: Long = 0xFF000000L,
    val defaultMirrorMode: MirrorMode = MirrorMode.NORMAL,
    val defaultScrollDirection: ScrollDirection = ScrollDirection.UP,
    val defaultCameraLens: CameraLensOption = CameraLensOption.FRONT,
    val defaultVideoResolution: VideoResolutionOption = VideoResolutionOption.FHD_1080,
    val defaultVideoFps: VideoFpsOption = VideoFpsOption.FPS_30,
    val countdownSeconds: Int = 3,
    val keepScreenOn: Boolean = true,
    val cameraOverlayOpacity: Float = 0.58f,
    val remoteBindings: RemoteKeyBindings = RemoteKeyBindings()
)
