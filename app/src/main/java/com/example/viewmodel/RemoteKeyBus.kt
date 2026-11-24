package com.example.viewmodel

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local in-memory bus forwarding hardware key events (Bluetooth keyboards, presentation remotes,
 * external keyboards, and volume buttons) from Activity.dispatchKeyEvent to active screens.
 */
object RemoteKeyBus {
    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val events: SharedFlow<Int> = _events.asSharedFlow()

    val isTeleprompterActive = AtomicBoolean(false)
    val isCapturingShortcut = AtomicBoolean(false)
    val volumeControlEnabled = AtomicBoolean(true)

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.repeatCount > 0 &&
            keyCode != KeyEvent.KEYCODE_DPAD_UP &&
            keyCode != KeyEvent.KEYCODE_DPAD_DOWN &&
            keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
            keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
        ) {
            return false
        }

        if (isCapturingShortcut.get()) {
            if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_HOME) {
                _events.tryEmit(keyCode)
                return true
            }
            return false
        }

        if (!isTeleprompterActive.get()) return false

        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && !volumeControlEnabled.get()) {
            return false
        }

        // Intercept common remote / keyboard keys when actively in teleprompter reading or camera mode
        val shouldHandle = isVolumeKey ||
            keyCode == KeyEvent.KEYCODE_SPACE ||
            keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            keyCode == KeyEvent.KEYCODE_PAGE_UP ||
            keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_ESCAPE ||
            (keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) ||
            (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9)

        if (shouldHandle) {
            _events.tryEmit(keyCode)
            return true
        }
        return false
    }
}
