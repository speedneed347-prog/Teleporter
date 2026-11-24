package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.AppSettings
import com.example.data.model.ScriptEntity
import com.example.data.repository.ScriptRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveStatus {
    IDLE, SAVING, SAVED, ERROR
}

data class EditorUiState(
    val scriptId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val existingEntity: ScriptEntity? = null
) {
    val wordCount: Int
        get() {
            val t = content.trim()
            return if (t.isEmpty()) 0 else t.split(Regex("\\s+")).count { it.isNotBlank() }
        }

    val charCount: Int get() = content.length

    val estimatedMinutesText: String
        get() {
            val words = wordCount
            if (words == 0) return "0:00"
            val totalSec = ((words * 60f) / 135f).toInt().coerceAtLeast(1)
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }
}

class EditorViewModel(
    private val scriptRepository: ScriptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var currentDefaults: AppSettings = AppSettings()

    fun loadScript(scriptId: Long, defaults: AppSettings) {
        currentDefaults = defaults
        if (scriptId <= 0L) {
            _uiState.value = EditorUiState(
                scriptId = 0L,
                title = "",
                content = "",
                saveStatus = SaveStatus.IDLE,
                isLoading = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val entity = scriptRepository.getScriptById(scriptId)
            if (entity != null) {
                _uiState.value = EditorUiState(
                    scriptId = entity.id,
                    title = entity.title,
                    content = entity.content,
                    saveStatus = SaveStatus.SAVED,
                    isLoading = false,
                    existingEntity = entity
                )
            } else {
                _uiState.value = EditorUiState(
                    isLoading = false,
                    errorMessage = "Script could not be found."
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, saveStatus = SaveStatus.SAVING) }
        scheduleAutoSave()
    }

    fun onContentChange(newContent: String) {
        _uiState.update { it.copy(content = newContent, saveStatus = SaveStatus.SAVING) }
        scheduleAutoSave()
    }

    fun appendOrPasteText(pasted: String) {
        if (pasted.isBlank()) return
        val current = _uiState.value.content
        val combined = if (current.isBlank()) pasted else "$current\n\n$pasted"
        val suggestedTitle = if (_uiState.value.title.isBlank()) {
            pasted.lineSequence().firstOrNull { it.isNotBlank() }?.take(42)?.trim().orEmpty()
        } else {
            _uiState.value.title
        }
        _uiState.update {
            it.copy(
                title = suggestedTitle,
                content = combined,
                saveStatus = SaveStatus.SAVING
            )
        }
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(750L)
            persistNowInternal()
        }
    }

    fun saveNow(onSaved: (Long) -> Unit = {}) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            val savedId = persistNowInternal()
            if (savedId > 0L) onSaved(savedId)
        }
    }

    fun duplicateCurrentScript(onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = persistNowInternal()
            val entity = if (id > 0L) scriptRepository.getScriptById(id) else null
            if (entity != null) {
                val copyId = scriptRepository.duplicateScript(entity)
                onDuplicated(copyId)
            }
        }
    }

    fun deleteCurrentScript(onDeleted: () -> Unit) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            val id = _uiState.value.scriptId
            if (id > 0L) {
                scriptRepository.deleteScriptById(id)
            }
            onDeleted()
        }
    }

    private suspend fun persistNowInternal(): Long {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) {
            _uiState.update { it.copy(saveStatus = SaveStatus.IDLE) }
            return state.scriptId
        }
        return try {
            val cleanTitle = state.title.trim().ifEmpty {
                state.content.lineSequence()
                    .firstOrNull { it.isNotBlank() }
                    ?.take(40)
                    ?.trim()
                    ?: "Untitled Script"
            }
            if (state.scriptId <= 0L) {
                val newId = scriptRepository.createScript(
                    title = cleanTitle,
                    content = state.content,
                    defaults = currentDefaults
                )
                val inserted = scriptRepository.getScriptById(newId)
                _uiState.update {
                    it.copy(
                        scriptId = newId,
                        title = if (it.title.isBlank()) cleanTitle else it.title,
                        existingEntity = inserted,
                        saveStatus = SaveStatus.SAVED,
                        errorMessage = null
                    )
                }
                newId
            } else {
                val base = state.existingEntity ?: scriptRepository.getScriptById(state.scriptId)
                if (base != null) {
                    val updated = base.copy(
                        title = cleanTitle,
                        content = state.content,
                        modifiedAt = System.currentTimeMillis()
                    )
                    scriptRepository.updateScript(updated)
                    _uiState.update {
                        it.copy(
                            existingEntity = updated,
                            saveStatus = SaveStatus.SAVED,
                            errorMessage = null
                        )
                    }
                }
                state.scriptId
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    saveStatus = SaveStatus.ERROR,
                    errorMessage = "Auto-save failed: ${e.localizedMessage ?: "Storage error"}"
                )
            }
            state.scriptId
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditorViewModel(container.scriptRepository) as T
                }
            }
    }
}
