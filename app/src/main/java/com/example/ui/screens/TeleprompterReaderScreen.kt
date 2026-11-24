package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSettings
import com.example.data.model.MirrorMode
import com.example.data.model.PrompterAlignment
import com.example.data.model.ScrollDirection
import com.example.ui.components.ScriptChunker
import com.example.ui.components.StudioEmptyState
import com.example.ui.components.TeleprompterProgressInfo
import com.example.ui.components.TeleprompterStyleBottomSheet
import com.example.ui.components.TeleprompterStyleConfig
import com.example.ui.components.TeleprompterViewport
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.TallyCrimson
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.RemoteKeyBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TeleprompterReaderScreen(
    scriptId: Long,
    settings: AppSettings,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onEditScript: (Long) -> Unit,
    onOpenCamera: (Long) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val quickText by mainViewModel.quickScriptText.collectAsStateWithLifecycle()
    val scriptFlow = remember(scriptId) {
        if (scriptId > 0L) mainViewModel.observeScript(scriptId) else null
    }
    val scriptEntity = scriptFlow?.collectAsStateWithLifecycle(initialValue = null)?.value

    val scriptTitle = if (scriptId > 0L) scriptEntity?.title.orEmpty() else "Quick Teleprompter"
    val scriptContent = if (scriptId > 0L) scriptEntity?.content.orEmpty() else quickText

    val chunks = remember(scriptContent) { ScriptChunker.splitIntoParagraphChunks(scriptContent) }
    val totalWords = remember(scriptContent) {
        val trimmed = scriptContent.trim()
        if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    var style by remember {
        mutableStateOf(
            TeleprompterStyleConfig(
                fontSizeSp = settings.defaultFontSizeSp,
                scrollSpeed = settings.defaultScrollSpeed,
                lineSpacingMultiplier = settings.defaultLineSpacing,
                paragraphSpacingDp = settings.defaultParagraphSpacingDp,
                horizontalMarginDp = settings.defaultHorizontalMarginDp,
                readingAreaWidthFraction = settings.defaultReadingAreaWidthFraction,
                alignment = settings.defaultAlignment,
                textColor = Color(settings.defaultTextColorHex),
                backgroundColor = Color(settings.defaultBackgroundColorHex),
                mirrorMode = settings.defaultMirrorMode,
                scrollDirection = settings.defaultScrollDirection
            )
        )
    }
    var styleInitializedForId by remember { mutableStateOf(-999L) }
    var positionRestoredForId by remember { mutableStateOf(-999L) }

    val listState = rememberLazyListState()
    var isPlaying by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showStyleSheet by remember { mutableStateOf(false) }
    var countdownSeconds by remember(settings.countdownSeconds) { mutableIntStateOf(settings.countdownSeconds) }
    var activeCountdown by remember { mutableIntStateOf(0) }
    var progressInfo by remember { mutableStateOf(TeleprompterProgressInfo(0f, 0, chunks.size, totalWords, 0)) }
    var orientationLockedLandscape by remember { mutableStateOf(false) }

    // Hydrate per-script style & saved scroll position when entity loads
    LaunchedEffect(scriptEntity?.id) {
        val entity = scriptEntity ?: return@LaunchedEffect
        if (styleInitializedForId != entity.id) {
            styleInitializedForId = entity.id
            style = TeleprompterStyleConfig(
                fontSizeSp = entity.fontSizeSp.coerceIn(16f, 72f),
                scrollSpeed = entity.scrollSpeed.coerceIn(1, 300),
                lineSpacingMultiplier = entity.lineSpacingMultiplier.coerceIn(1.0f, 2.5f),
                paragraphSpacingDp = entity.paragraphSpacingDp.coerceIn(4, 56),
                horizontalMarginDp = entity.horizontalMarginDp.coerceIn(0, 96),
                readingAreaWidthFraction = entity.readingAreaWidthFraction.coerceIn(0.5f, 1.0f),
                alignment = runCatching { PrompterAlignment.valueOf(entity.textAlignment) }
                    .getOrDefault(settings.defaultAlignment),
                textColor = Color(entity.textColorHex),
                backgroundColor = Color(entity.backgroundColorHex),
                mirrorMode = runCatching { MirrorMode.valueOf(entity.mirrorMode) }
                    .getOrDefault(settings.defaultMirrorMode),
                scrollDirection = runCatching { ScrollDirection.valueOf(entity.scrollDirection) }
                    .getOrDefault(settings.defaultScrollDirection)
            )
        }
        if (positionRestoredForId != entity.id && chunks.isNotEmpty()) {
            positionRestoredForId = entity.id
            val targetIdx = entity.lastReadingParagraphIndex.coerceIn(0, chunks.size + 1)
            listState.scrollToItem(targetIdx, entity.lastReadingScrollOffsetPx.coerceAtLeast(0))
        }
    }

    // Persist reading position & config helper
    val persistState: () -> Unit = {
        if (scriptId > 0L) {
            mainViewModel.saveReadingPosition(
                id = scriptId,
                paragraphIndex = listState.firstVisibleItemIndex,
                scrollOffsetPx = listState.firstVisibleItemScrollOffset,
                progress = progressInfo.progress
            )
            mainViewModel.saveScriptReadingConfig(
                id = scriptId,
                fontSizeSp = style.fontSizeSp,
                scrollSpeed = style.scrollSpeed,
                lineSpacingMultiplier = style.lineSpacingMultiplier,
                paragraphSpacingDp = style.paragraphSpacingDp,
                horizontalMarginDp = style.horizontalMarginDp,
                textAlignment = style.alignment,
                textColorHex = style.textColor.toArgb().toLong() and 0xFFFFFFFFL,
                backgroundColorHex = style.backgroundColor.toArgb().toLong() and 0xFFFFFFFFL,
                mirrorMode = style.mirrorMode,
                scrollDirection = style.scrollDirection,
                readingAreaWidthFraction = style.readingAreaWidthFraction
            )
        }
    }

    // Countdown coroutine when user starts playback
    LaunchedEffect(activeCountdown) {
        if (activeCountdown > 0) {
            delay(1000L)
            if (activeCountdown == 1) {
                activeCountdown = 0
                isPlaying = true
            } else {
                activeCountdown -= 1
            }
        }
    }

    val togglePlayPause: () -> Unit = {
        if (isPlaying) {
            isPlaying = false
            persistState()
        } else if (activeCountdown > 0) {
            activeCountdown = 0
        } else if (chunks.isNotEmpty()) {
            if (countdownSeconds > 0 && progressInfo.progress < 0.02f) {
                activeCountdown = countdownSeconds
            } else {
                isPlaying = true
            }
        }
    }

    val restartScript: () -> Unit = {
        activeCountdown = 0
        scope.launch {
            listState.scrollToItem(0, 0)
            persistState()
        }
    }

    val stopScript: () -> Unit = {
        isPlaying = false
        activeCountdown = 0
        scope.launch {
            listState.scrollToItem(0, 0)
            persistState()
        }
    }

    // Keep screen on + immersive fullscreen + remote key activation
    DisposableEffect(settings.keepScreenOn, settings.remoteBindings.volumeButtonsEnabled) {
        RemoteKeyBus.isTeleprompterActive.set(true)
        RemoteKeyBus.volumeControlEnabled.set(settings.remoteBindings.volumeButtonsEnabled)
        if (settings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            RemoteKeyBus.isTeleprompterActive.set(false)
            persistState()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Toggle immersive system bars when controls are hidden
    LaunchedEffect(controlsVisible) {
        activity?.window?.let { win ->
            val controller = WindowCompat.getInsetsController(win, win.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (controlsVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Listen to Bluetooth Remote / External Keyboard / Volume keys
    LaunchedEffect(settings.remoteBindings) {
        val bindings = settings.remoteBindings
        RemoteKeyBus.events.collectLatest { keyCode ->
            when {
                keyCode == bindings.playPauseKeyCode ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    keyCode == KeyEvent.KEYCODE_ENTER -> {
                    togglePlayPause()
                }

                keyCode == bindings.speedUpKeyCode ||
                    keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                    (bindings.volumeButtonsEnabled && keyCode == KeyEvent.KEYCODE_VOLUME_UP) -> {
                    style = style.copy(scrollSpeed = (style.scrollSpeed + 10).coerceAtMost(300))
                }

                keyCode == bindings.speedDownKeyCode ||
                    keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                    (bindings.volumeButtonsEnabled && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) -> {
                    style = style.copy(scrollSpeed = (style.scrollSpeed - 10).coerceAtLeast(1))
                }

                keyCode == bindings.restartKeyCode -> {
                    restartScript()
                }

                keyCode == bindings.exitKeyCode -> {
                    if (!controlsVisible) {
                        controlsVisible = true
                    } else {
                        persistState()
                        onNavigateBack()
                    }
                }
            }
        }
    }

    BackHandler {
        persistState()
        onNavigateBack()
    }

    if (chunks.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                StudioEmptyState(
                    icon = Icons.Default.Edit,
                    title = "Empty Script",
                    subtitle = "Add or paste text into your script before starting the teleprompter.",
                    primaryActionLabel = if (scriptId > 0L) "Open Script Editor" else "Go Back",
                    onPrimaryAction = {
                        if (scriptId > 0L) onEditScript(scriptId) else onNavigateBack()
                    }
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(style.backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
            .testTag("teleprompter_viewport_root")
    ) {
        TeleprompterViewport(
            chunks = chunks,
            totalWordCount = totalWords,
            listState = listState,
            style = style,
            isPlaying = isPlaying,
            showReadingGuide = true,
            guideVerticalBias = 0.42f,
            transparentBackground = false,
            onReachedEnd = {
                isPlaying = false
                persistState()
            },
            onProgressChanged = { progressInfo = it }
        )

        // Countdown Overlay
        AnimatedVisibility(
            visible = activeCountdown > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(3.dp, BroadcastAmber),
                modifier = Modifier.size(132.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeCountdown.toString(),
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 62.sp,
                        color = BroadcastAmber
                    )
                }
            }
        }

        // Top Floating Studio Header Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color(0xCC0C0E13),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                persistState()
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Teleprompter",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scriptTitle.ifBlank { "Teleprompter" },
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Mirror: ${style.mirrorMode.shortLabel} · Speed ${style.scrollSpeed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastAmber
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Mirror quick cycle
                        IconButton(
                            onClick = {
                                val next = MirrorMode.entries[(style.mirrorMode.ordinal + 1) % MirrorMode.entries.size]
                                style = style.copy(mirrorMode = next)
                            },
                            modifier = Modifier.testTag("reader_mirror_button")
                        ) {
                            Icon(
                                Icons.Default.Flip,
                                contentDescription = "Toggle Mirror Mode",
                                tint = if (style.mirrorMode != MirrorMode.NORMAL) BroadcastAmber else Color.White
                            )
                        }

                        // Orientation switch (Portrait / Landscape)
                        IconButton(
                            onClick = {
                                orientationLockedLandscape = !orientationLockedLandscape
                                activity?.requestedOrientation = if (orientationLockedLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            },
                            modifier = Modifier.testTag("reader_rotate_button")
                        ) {
                            Icon(
                                Icons.Default.ScreenRotation,
                                contentDescription = "Toggle Portrait or Landscape",
                                tint = Color.White
                            )
                        }

                        // Camera mode shortcut
                        IconButton(
                            onClick = {
                                persistState()
                                onOpenCamera(scriptId)
                            },
                            modifier = Modifier.testTag("reader_switch_camera_button")
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = "Open Camera Teleprompter",
                                tint = TallyCrimson
                            )
                        }

                        // Style / Appearance bottom sheet
                        IconButton(
                            onClick = { showStyleSheet = true },
                            modifier = Modifier.testTag("reader_style_button")
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Customize appearance",
                                tint = Color.White
                            )
                        }

                        // Hide controls button
                        IconButton(
                            onClick = { controlsVisible = false },
                            modifier = Modifier.testTag("reader_hide_controls_button")
                        ) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = "Hide controls",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Minimal corner reveal chip when controls are hidden
        AnimatedVisibility(
            visible = !controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = { controlsVisible = true },
                    modifier = Modifier.testTag("reader_show_controls_button")
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Show controls",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Bottom Floating Control Deck
        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xE8121620),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Progress & remaining text telemetry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(progressInfo.progress * 100).roundToInt()}% · Para ${progressInfo.currentChunkIndex + 1}/${progressInfo.totalChunks.coerceAtLeast(1)}",
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            color = BroadcastAmber
                        )
                        Text(
                            text = "${progressInfo.estimatedWordsRemaining}w left · ${formatDurationShort(progressInfo.estimatedSecondsRemaining)}",
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFB8C2D4)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressInfo.progress.coerceIn(0f, 1f) },
                        color = BroadcastAmber,
                        trackColor = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    // Speed & Font quick adjusters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    style = style.copy(scrollSpeed = (style.scrollSpeed - 5).coerceAtLeast(1))
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("reader_speed_down_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease speed", tint = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SPEED", fontSize = 9.sp, color = Color(0xFF9CA8BC))
                                Text(
                                    text = "${style.scrollSpeed}",
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BroadcastAmber
                                )
                            }
                            IconButton(
                                onClick = {
                                    style = style.copy(scrollSpeed = (style.scrollSpeed + 5).coerceAtMost(300))
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("reader_speed_up_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase speed", tint = Color.White)
                            }
                        }

                        // Font size control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    style = style.copy(fontSizeSp = (style.fontSizeSp - 2f).coerceAtLeast(16f))
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("reader_font_down_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease font size", tint = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FONT", fontSize = 9.sp, color = Color(0xFF9CA8BC))
                                Text(
                                    text = "${style.fontSizeSp.roundToInt()}sp",
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    style = style.copy(fontSizeSp = (style.fontSizeSp + 2f).coerceAtMost(72f))
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("reader_font_up_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase font size", tint = Color.White)
                            }
                        }
                    }

                    // Transport Controls: Restart | Play/Pause | Stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = restartScript,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("reader_restart_button")
                        ) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = "Restart script",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = togglePlayPause,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = BroadcastAmber,
                                contentColor = Color(0xFF1A1100)
                            ),
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("reader_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause scrolling" else "Start scrolling",
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        IconButton(
                            onClick = stopScript,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("reader_stop_button")
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop and reset",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStyleSheet) {
        TeleprompterStyleBottomSheet(
            style = style,
            countdownSeconds = countdownSeconds,
            showBackgroundColors = true,
            onStyleChange = { updated -> style = updated },
            onCountdownChange = { countdownSeconds = it },
            onDismiss = {
                showStyleSheet = false
                persistState()
            }
        )
    }
}
