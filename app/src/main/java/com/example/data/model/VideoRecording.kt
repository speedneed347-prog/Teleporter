package com.example.data.model

import android.net.Uri
import java.util.Locale

data class VideoRecording(
    val id: String,
    val uri: Uri,
    val filePath: String?,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val isMediaStore: Boolean
) {
    fun formattedDuration(): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formattedResolution(): String {
        return if (width > 0 && height > 0) "${width}×${height}" else "HD Video"
    }

    fun formattedFileSize(): String {
        return when {
            sizeBytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", sizeBytes / 1_073_741_824.0)
            sizeBytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", sizeBytes / 1_048_576.0)
            sizeBytes >= 1_024L -> String.format(Locale.US, "%.0f KB", sizeBytes / 1_024.0)
            else -> "$sizeBytes B"
        }
    }
}
