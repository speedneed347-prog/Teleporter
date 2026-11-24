package com.example.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.VideoRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(private val context: Context) {

    companion object {
        const val RELATIVE_MOVIE_DIR = "Movies/SmartTeleprompterPro"
        const val FILENAME_PREFIX = "STP_"
    }

    fun getAppLocalVideosDir(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "videos")
        if (!baseDir.exists()) baseDir.mkdirs()
        return baseDir
    }

    suspend fun loadRecordings(): List<VideoRecording> = withContext(Dispatchers.IO) {
        val result = mutableListOf<VideoRecording>()
        val seenUris = HashSet<String>()

        // 1. Query MediaStore entries created by this app (Movies/SmartTeleprompterPro)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.RELATIVE_PATH
            )
            val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%SmartTeleprompterPro%")
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            runCatching {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val uriStr = contentUri.toString()
                        if (seenUris.add(uriStr)) {
                            var dur = cursor.getLong(durCol)
                            var w = cursor.getInt(widthCol)
                            var h = cursor.getInt(heightCol)
                            if (dur <= 0L || w <= 0 || h <= 0) {
                                val probed = probeMetadata(contentUri)
                                if (dur <= 0L) dur = probed.first
                                if (w <= 0) w = probed.second
                                if (h <= 0) h = probed.third
                            }
                            result.add(
                                VideoRecording(
                                    id = "ms_$id",
                                    uri = contentUri,
                                    filePath = null,
                                    displayName = cursor.getString(nameCol) ?: "Recording_$id.mp4",
                                    durationMs = dur,
                                    width = w,
                                    height = h,
                                    sizeBytes = cursor.getLong(sizeCol),
                                    dateAddedSeconds = cursor.getLong(dateCol),
                                    isMediaStore = true
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Also include local app-private recorded MP4 files (API 24-28 or fallback saves)
        val localDir = getAppLocalVideosDir()
        val files = localDir.listFiles { file ->
            file.isFile && file.extension.equals("mp4", ignoreCase = true) && file.length() > 0L
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        for (file in files) {
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            if (seenUris.add(shareUri.toString())) {
                val (dur, w, h) = probeMetadata(shareUri)
                result.add(
                    VideoRecording(
                        id = "file_${file.absolutePath}",
                        uri = shareUri,
                        filePath = file.absolutePath,
                        displayName = file.name,
                        durationMs = dur,
                        width = w,
                        height = h,
                        sizeBytes = file.length(),
                        dateAddedSeconds = file.lastModified() / 1000L,
                        isMediaStore = false
                    )
                )
            }
        }

        result.sortedByDescending { it.dateAddedSeconds }
    }

    suspend fun loadThumbnail(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun renameRecording(recording: VideoRecording, rawNewName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val sanitized = rawNewName.trim()
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .removeSuffix(".mp4")
                .trim()
            if (sanitized.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Video name cannot be empty."))
            }
            val finalName = "$sanitized.mp4"

            runCatching {
                if (recording.isMediaStore) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, finalName)
                    }
                    val updated = context.contentResolver.update(recording.uri, values, null, null)
                    if (updated <= 0) {
                        throw IllegalStateException("System denied renaming this MediaStore video.")
                    }
                } else {
                    val oldFile = recording.filePath?.let { File(it) }
                        ?: throw IllegalStateException("Local file path unavailable.")
                    val target = File(oldFile.parentFile, finalName)
                    if (target.exists() && target.absolutePath != oldFile.absolutePath) {
                        throw IllegalStateException("A video named \"$finalName\" already exists.")
                    }
                    if (!oldFile.renameTo(target)) {
                        throw IllegalStateException("Could not rename local video file.")
                    }
                }
            }
        }

    suspend fun deleteRecording(recording: VideoRecording): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (recording.isMediaStore) {
                val deleted = context.contentResolver.delete(recording.uri, null, null)
                if (deleted <= 0) {
                    throw IllegalStateException("Could not remove recording from MediaStore.")
                }
            } else {
                val file = recording.filePath?.let { File(it) }
                    ?: throw IllegalStateException("Local file path unavailable.")
                if (file.exists() && !file.delete()) {
                    throw IllegalStateException("Could not delete local recording file.")
                }
            }
        }
    }

    private fun probeMetadata(uri: Uri): Triple<Long, Int, Int> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            Triple(duration, width, height)
        } catch (_: Exception) {
            Triple(0L, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
    }
}
