package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.model.VideoRecording
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoGalleryUiState(
    val videos: List<VideoRecording> = emptyList(),
    val thumbnails: Map<String, Bitmap> = emptyMap(),
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class VideoGalleryViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoGalleryUiState())
    val uiState: StateFlow<VideoGalleryUiState> = _uiState.asStateFlow()

    init {
        refreshVideos()
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val list = videoRepository.loadRecordings()
                _uiState.update { it.copy(videos = list, isLoading = false) }
                // Load local thumbnails asynchronously
                list.forEach { video ->
                    if (!_uiState.value.thumbnails.containsKey(video.id)) {
                        launch {
                            val bmp = videoRepository.loadThumbnail(video.uri)
                            if (bmp != null) {
                                _uiState.update { state ->
                                    state.copy(thumbnails = state.thumbnails + (video.id to bmp))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to scan local recordings: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun renameVideo(recording: VideoRecording, newName: String) {
        viewModelScope.launch {
            videoRepository.renameRecording(recording, newName)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Video renamed successfully") }
                    refreshVideos()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Rename failed") }
                }
        }
    }

    fun deleteVideo(recording: VideoRecording) {
        viewModelScope.launch {
            videoRepository.deleteRecording(recording)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Video deleted from device") }
                    refreshVideos()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Delete failed") }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VideoGalleryViewModel(container.videoRepository) as T
                }
            }
    }
}
