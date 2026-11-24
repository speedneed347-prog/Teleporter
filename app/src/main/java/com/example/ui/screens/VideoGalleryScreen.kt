package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VideoRecording
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RenameItemDialog
import com.example.ui.components.StudioEmptyState
import com.example.ui.components.formatDurationShort
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.VideoGalleryViewModel
import kotlinx.coroutines.delay

@Composable
fun VideoGalleryScreen(
    viewModel: VideoGalleryViewModel,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var playingVideo by remember { mutableStateOf<VideoRecording?>(null) }
    var videoToRename by remember { mutableStateOf<VideoRecording?>(null) }
    var videoToDelete by remember { mutableStateOf<VideoRecording?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshVideos()
    }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.statusMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val shareVideo: (VideoRecording) -> Unit = { video ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, video.uri)
            putExtra(Intent.EXTRA_SUBJECT, video.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Recorded Video"))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recorded Videos",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${uiState.videos.size} local video${if (uiState.videos.size == 1) "" else "s"} · Stored on-device only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(
                        onClick = viewModel::refreshVideos,
                        modifier = Modifier.testTag("gallery_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh videos")
                    }
                    IconButton(
                        onClick = onOpenCamera,
                        modifier = Modifier.testTag("gallery_record_new_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Record new video")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.videos.isEmpty() -> {
                    StudioEmptyState(
                        icon = Icons.Default.VideoLibrary,
                        title = "No Recorded Videos Yet",
                        subtitle = "Record your first take in Camera Teleprompter. All videos are saved privately on your Android device and never uploaded.",
                        primaryActionLabel = "Open Camera Teleprompter",
                        onPrimaryAction = onOpenCamera,
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.videos, key = { it.id }) { video ->
                            VideoRecordingCard(
                                video = video,
                                thumbnail = uiState.thumbnails[video.id],
                                onPlay = { playingVideo = video },
                                onRename = { videoToRename = video },
                                onShare = { shareVideo(video) },
                                onDelete = { videoToDelete = video }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }

    playingVideo?.let { video ->
        LocalVideoPlayerDialog(
            video = video,
            onShare = { shareVideo(video) },
            onDismiss = { playingVideo = null }
        )
    }

    videoToRename?.let { target ->
        RenameItemDialog(
            dialogTitle = "Rename Video",
            initialValue = target.displayName.removeSuffix(".mp4"),
            label = "Video Name",
            onConfirm = { newName ->
                viewModel.renameVideo(target, newName)
                videoToRename = null
            },
            onDismiss = { videoToRename = null }
        )
    }

    videoToDelete?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Recording?",
            message = "Permanently remove \"${target.displayName}\" (${target.formattedFileSize()}) from your device?",
            onConfirm = {
                viewModel.deleteVideo(target)
                videoToDelete = null
            },
            onDismiss = { videoToDelete = null }
        )
    }
}

@Composable
private fun VideoRecordingCard(
    video: VideoRecording,
    thumbnail: Bitmap?,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlay)
            .testTag("video_card_${video.id}")
    ) {
        Column {
            // Thumbnail Preview Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .background(Color(0xFF0A0D12)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Thumbnail for ${video.displayName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Play badge
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.5.dp, BroadcastAmber),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            tint = BroadcastAmber,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Duration & Resolution badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Text(
                            text = video.formattedResolution(),
                            fontFamily = MonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Text(
                            text = video.formattedDuration(),
                            fontFamily = MonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastAmber,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Metadata + Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${video.formattedFileSize()} · ${formatRelativeDate(video.dateAddedSeconds * 1000L)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename video")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share video")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete video",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalVideoPlayerDialog(
    video: VideoRecording,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(video.durationMs.toInt().coerceAtLeast(1000)) }

    LaunchedEffect(isPlaying, videoViewRef) {
        while (true) {
            videoViewRef?.let { vv ->
                if (vv.duration > 0) totalDurationMs = vv.duration
                currentPosMs = vv.currentPosition.coerceAtLeast(0)
                isPlaying = vv.isPlaying
            }
            delay(250L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${video.formattedResolution()} · ${video.formattedFileSize()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB8C2D4)
                        )
                    }
                    Row {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close player", tint = Color.White)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(video.uri)
                                setOnPreparedListener { mp ->
                                    totalDurationMs = mp.duration.coerceAtLeast(1000)
                                    start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Slider(
                        value = currentPosMs.toFloat().coerceIn(0f, totalDurationMs.toFloat()),
                        onValueChange = { pos ->
                            currentPosMs = pos.toInt()
                            videoViewRef?.seekTo(currentPosMs)
                        },
                        valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDurationShort(currentPosMs / 1000),
                            fontFamily = MonoFontFamily,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                        IconButton(
                            onClick = {
                                val vv = videoViewRef ?: return@IconButton
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = BroadcastAmber,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = formatDurationShort(totalDurationMs / 1000),
                            fontFamily = MonoFontFamily,
                            color = Color(0xFFB8C2D4),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
