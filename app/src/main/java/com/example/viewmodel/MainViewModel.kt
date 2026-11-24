package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppSettings
import com.example.data.model.AppThemeMode
import com.example.data.model.CameraLensOption
import com.example.data.model.MirrorMode
import com.example.data.model.PrompterAlignment
import com.example.data.model.RemoteAction
import com.example.data.model.ScriptEntity
import com.example.data.model.ScrollDirection
import com.example.data.model.VideoFpsOption
import com.example.data.model.VideoResolutionOption
import com.example.data.repository.ScriptRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScriptSortOrder(val label: String) {
    MODIFIED("Recently Edited"),
    CREATED("Date Created"),
    TITLE("Title A–Z")
}

class MainViewModel(
    private val scriptRepository: ScriptRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(ScriptSortOrder.MODIFIED)
    val sortOrder: StateFlow<ScriptSortOrder> = _sortOrder.asStateFlow()

    val recentScripts: StateFlow<List<ScriptEntity>> = scriptRepository.observeRecentScripts(6)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allScripts: StateFlow<List<ScriptEntity>> = _searchQuery
        .flatMapLatest { query -> scriptRepository.searchScripts(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Quick teleprompter ephemeral script buffer (in-memory, optionally saveable)
    private val _quickScriptText = MutableStateFlow("")
    val quickScriptText: StateFlow<String> = _quickScriptText.asStateFlow()

    fun updateQuickScriptText(text: String) {
        _quickScriptText.value = text
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: ScriptSortOrder) {
        _sortOrder.value = order
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
        }
    }

    fun createNewScript(title: String, content: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = scriptRepository.createScript(title, content, settings.value)
            onCreated(id)
        }
    }

    fun renameScript(id: Long, newTitle: String) {
        viewModelScope.launch {
            scriptRepository.renameScript(id, newTitle)
        }
    }

    fun duplicateScript(script: ScriptEntity, onDuplicated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newId = scriptRepository.duplicateScript(script)
            onDuplicated(newId)
        }
    }

    fun deleteScript(id: Long) {
        viewModelScope.launch {
            scriptRepository.deleteScriptById(id)
        }
    }

    fun saveReadingPosition(id: Long, paragraphIndex: Int, scrollOffsetPx: Int, progress: Float) {
        if (id <= 0L) return
        viewModelScope.launch {
            scriptRepository.saveReadingPosition(id, paragraphIndex, scrollOffsetPx, progress)
        }
    }

    fun saveScriptReadingConfig(
        id: Long,
        fontSizeSp: Float,
        scrollSpeed: Int,
        lineSpacingMultiplier: Float,
        paragraphSpacingDp: Int,
        horizontalMarginDp: Int,
        textAlignment: PrompterAlignment,
        textColorHex: Long,
        backgroundColorHex: Long,
        mirrorMode: MirrorMode,
        scrollDirection: ScrollDirection,
        readingAreaWidthFraction: Float
    ) {
        if (id <= 0L) return
        viewModelScope.launch {
            scriptRepository.saveReadingConfig(
                id = id,
                fontSizeSp = fontSizeSp,
                scrollSpeed = scrollSpeed,
                lineSpacingMultiplier = lineSpacingMultiplier,
                paragraphSpacingDp = paragraphSpacingDp,
                horizontalMarginDp = horizontalMarginDp,
                textAlignment = textAlignment.name,
                textColorHex = textColorHex,
                backgroundColorHex = backgroundColorHex,
                mirrorMode = mirrorMode.name,
                scrollDirection = scrollDirection.name,
                readingAreaWidthFraction = readingAreaWidthFraction
            )
        }
    }

    fun observeScript(id: Long) = scriptRepository.observeScriptById(id)

    suspend fun getMostRecentScript(): ScriptEntity? = scriptRepository.getMostRecentScript()

    // Settings updates
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settingsDataStore.updateThemeMode(mode) }
    fun setDefaultSpeed(speed: Int) = viewModelScope.launch { settingsDataStore.updateDefaultSpeed(speed) }
    fun setDefaultFontSize(sizeSp: Float) = viewModelScope.launch { settingsDataStore.updateDefaultFontSize(sizeSp) }
    fun setDefaultLineSpacing(mult: Float) = viewModelScope.launch { settingsDataStore.updateDefaultLineSpacing(mult) }
    fun setDefaultParagraphSpacing(dp: Int) = viewModelScope.launch { settingsDataStore.updateDefaultParagraphSpacing(dp) }
    fun setDefaultMargin(dp: Int) = viewModelScope.launch { settingsDataStore.updateDefaultMargin(dp) }
    fun setDefaultWidthFraction(frac: Float) = viewModelScope.launch { settingsDataStore.updateDefaultWidthFraction(frac) }
    fun setDefaultAlignment(alignment: PrompterAlignment) = viewModelScope.launch { settingsDataStore.updateDefaultAlignment(alignment) }
    fun setDefaultTextColor(hex: Long) = viewModelScope.launch { settingsDataStore.updateDefaultTextColor(hex) }
    fun setDefaultBackgroundColor(hex: Long) = viewModelScope.launch { settingsDataStore.updateDefaultBackgroundColor(hex) }
    fun setDefaultMirrorMode(mode: MirrorMode) = viewModelScope.launch { settingsDataStore.updateDefaultMirrorMode(mode) }
    fun setDefaultScrollDirection(dir: ScrollDirection) = viewModelScope.launch { settingsDataStore.updateDefaultScrollDirection(dir) }
    fun setDefaultCameraLens(lens: CameraLensOption) = viewModelScope.launch { settingsDataStore.updateDefaultCameraLens(lens) }
    fun setDefaultVideoResolution(res: VideoResolutionOption) = viewModelScope.launch { settingsDataStore.updateDefaultVideoResolution(res) }
    fun setDefaultVideoFps(fps: VideoFpsOption) = viewModelScope.launch { settingsDataStore.updateDefaultVideoFps(fps) }
    fun setCountdownSeconds(sec: Int) = viewModelScope.launch { settingsDataStore.updateCountdownSeconds(sec) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateKeepScreenOn(enabled) }
    fun setCameraOverlayOpacity(opacity: Float) = viewModelScope.launch { settingsDataStore.updateCameraOverlayOpacity(opacity) }
    fun setVolumeButtonsEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateVolumeButtonsEnabled(enabled) }
    fun setKeyBinding(action: RemoteAction, keyCode: Int) = viewModelScope.launch { settingsDataStore.updateKeyBinding(action, keyCode) }
    fun resetRemoteBindings() = viewModelScope.launch { settingsDataStore.resetRemoteBindings() }
    fun resetAllSettings() = viewModelScope.launch { settingsDataStore.resetAllSettings() }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(
                        scriptRepository = container.scriptRepository,
                        settingsDataStore = container.settingsDataStore
                    ) as T
                }
            }
    }
}
