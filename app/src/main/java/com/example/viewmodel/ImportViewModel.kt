package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.AppSettings
import com.example.data.repository.DocumentImportResult
import com.example.data.repository.DocumentImporter
import com.example.data.repository.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportUiState {
    data object Idle : ImportUiState()
    data object Extracting : ImportUiState()
    data class Preview(
        val title: String,
        val content: String,
        val formatLabel: String,
        val wordCount: Int,
        val charCount: Int
    ) : ImportUiState()

    data class Error(
        val headline: String,
        val message: String,
        val isScannedPdf: Boolean = false
    ) : ImportUiState()
}

class ImportViewModel(
    private val documentImporter: DocumentImporter,
    private val scriptRepository: ScriptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun importUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Extracting
            when (val result = documentImporter.importDocument(uri)) {
                is DocumentImportResult.Success -> {
                    _uiState.value = ImportUiState.Preview(
                        title = result.suggestedTitle,
                        content = result.extractedText,
                        formatLabel = result.formatLabel,
                        wordCount = result.wordCount,
                        charCount = result.characterCount
                    )
                }

                is DocumentImportResult.ScannedPdfError -> {
                    _uiState.value = ImportUiState.Error(
                        headline = "Scanned / Image-Only PDF Detected",
                        message = result.message,
                        isScannedPdf = true
                    )
                }

                is DocumentImportResult.UnsupportedFormatError -> {
                    _uiState.value = ImportUiState.Error(
                        headline = "Unsupported File Format",
                        message = result.message
                    )
                }

                is DocumentImportResult.EmptyDocumentError -> {
                    _uiState.value = ImportUiState.Error(
                        headline = "Empty Document",
                        message = result.message
                    )
                }

                is DocumentImportResult.ExtractionError -> {
                    _uiState.value = ImportUiState.Error(
                        headline = "Import Failed",
                        message = result.message
                    )
                }
            }
        }
    }

    fun updatePreviewTitle(title: String) {
        val current = _uiState.value
        if (current is ImportUiState.Preview) {
            _uiState.value = current.copy(title = title)
        }
    }

    fun updatePreviewContent(content: String) {
        val current = _uiState.value
        if (current is ImportUiState.Preview) {
            val words = content.trim().takeIf { it.isNotEmpty() }
                ?.split(Regex("\\s+"))?.count { it.isNotBlank() } ?: 0
            _uiState.value = current.copy(
                content = content,
                wordCount = words,
                charCount = content.length
            )
        }
    }

    fun saveImportedScript(defaults: AppSettings, onSaved: (Long) -> Unit) {
        val current = _uiState.value as? ImportUiState.Preview ?: return
        if (current.content.isBlank()) return
        viewModelScope.launch {
            val id = scriptRepository.createScript(
                title = current.title.ifBlank { "Imported Script" },
                content = current.content,
                defaults = defaults
            )
            _uiState.value = ImportUiState.Idle
            onSaved(id)
        }
    }

    fun reset() {
        _uiState.value = ImportUiState.Idle
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ImportViewModel(
                        documentImporter = container.documentImporter,
                        scriptRepository = container.scriptRepository
                    ) as T
                }
            }
    }
}
