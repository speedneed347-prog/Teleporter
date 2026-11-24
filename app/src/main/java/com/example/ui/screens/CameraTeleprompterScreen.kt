package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.provider.MediaStore
import android.util.Range
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSettings
import com.example.data.model.CameraLensOption
import com.example.data.model.MirrorMode
import com.example.data.model.ScriptEntity
import com.example.data.model.VideoFpsOption
import com.example.data.model.VideoResolutionOption
import com.example.data.repository.VideoRepository
import com.example.ui.components.ScriptChunker
import com.example.ui.components.TeleprompterProgressInfo
import com.example.ui.components.TeleprompterStyleBottomSheet
import com.example.ui.components.TeleprompterStyleConfig
import com.example.ui.components.TeleprompterViewport
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.TallyCrimson
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.RemoteKeyBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraTeleprompterScreen(
    initialScriptId: Long,
    settings: AppSettings,
    allScripts: List<ScriptEntity>,
    videoRepository: VideoRepository,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onOpenVideoGallery: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime permissions state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCameraPermission = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        permissionRequestedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    // Active script selection
    var activeScriptId by remember(initialScriptId) {
        mutableLongStateOf(
            if (initialScriptId > 0L) initialScriptId else allScripts.firstOrNull()?.id ?: -1L
        )
    }
    val quickText by mainViewModel.quickScriptText.collectAsStateWithLifecycle()
    val activeScript = remember(activeScriptId, allScripts) {
        allScripts.firstOrNull { it.id == activeScriptId }
    }
    val scriptContent = activeScript?.content ?: quickText
    val chunks = remember(scriptContent) { ScriptChunker.splitIntoParagraphChunks(scriptContent) }
    val totalWords = remember(scriptContent) {
        val t = scriptContent.trim()
        if (t.isEmpty()) 0 else t.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    // Independent Script Scrolling State
    val listState = rememberLazyListState()
    var isScrolling by remember { mutableStateOf(false) }
    var style by remember {
        mutableStateOf(
            TeleprompterStyleConfig(
                fontSizeSp = settings.defaultFontSizeSp.coerceIn(18f, 44f),
                scrollSpeed = settings.defaultScrollSpeed,
                lineSpacingMultiplier = settings.defaultLineSpacing,
                paragraphSpacingDp = settings.defaultParagraphSpacingDp,
                horizontalMarginDp = 14,
                readingAreaWidthFraction = 1.0f,
                alignment = settings.defaultAlignment,
                textColor = Color(settings.defaultTextColorHex),
                backgroundColor = Color.Transparent,
                mirrorMode = settings.defaultMirrorMode,
                scrollDirection = settings.defaultScrollDirection
            )
        )
    }
    var overlayOpacity by remember { mutableFloatStateOf(settings.cameraOverlayOpacity) }
    var countdownSeconds by remember { mutableIntStateOf(settings.countdownSeconds) }
    var progressInfo by remember { mutableStateOf(TeleprompterProgressInfo(0f, 0, chunks.size, totalWords, 0)) }

    // CameraX State
    var lensOption by remember { mutableStateOf(settings.defaultCameraLens) }
    var resolutionOption by remember { mutableStateOf(settings.defaultVideoResolution) }
    var fpsOption by remember { mutableStateOf(settings.defaultVideoFps) }
    var micEnabled by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Recording State (Independent from script scrolling)
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationNanos by remember { mutableLongStateOf(0L) }
    var activeCountdown by remember { mutableIntStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Sheets
    var showStyleSheet by remember { mutableStateOf(false) }
    var showCameraConfigSheet by remember { mutableStateOf(false) }
    var showScriptPickerSheet by remember { mutableStateOf(false) }

    // Keep screen on & Bluetooth remote activation
    DisposableEffect(settings.keepScreenOn, settings.remoteBindings.volumeButtonsEnabled) {
        RemoteKeyBus.isTeleprompterActive.set(true)
        RemoteKeyBus.volumeControlEnabled.set(settings.remoteBindings.volumeButtonsEnabled)
        if (settings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            RemoteKeyBus.isTeleprompterActive.set(false)
            runCatching { activeRecording?.stop() }
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Bind / Rebind CameraX whenever permission, lens, resolution, or FPS changes
    LaunchedEffect(hasCameraPermission, previewViewRef, lensOption, resolutionOption, fpsOption) {
        val previewView = previewViewRef ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    val cameraProvider = providerFuture.get()
                    val targetSelector = if (lensOption == CameraLensOption.FRONT) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val actualSelector = when {
                        cameraProvider.hasCamera(targetSelector) -> targetSelector
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> {
                            cameraError = "No camera hardware detected on this device."
                            return@addListener
                        }
                    }

                    val previewBuilder = Preview.Builder()
                    Camera2Interop.Extender(previewBuilder).setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        Range(fpsOption.fps, fpsOption.fps)
                    )
                    val preview = previewBuilder.build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val targetQuality = when (resolutionOption) {
                        VideoResolutionOption.SD_480 -> Quality.SD
                        VideoResolutionOption.HD_720 -> Quality.HD
                        VideoResolutionOption.FHD_1080 -> Quality.FHD
                        VideoResolutionOption.UHD_4K -> Quality.UHD
                    }
                    val qualitySelector = QualitySelector.from(
                        targetQuality,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )

                    val recorder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build()
                    val capture = VideoCapture.withOutput(recorder)

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        actualSelector,
                        preview,
                        capture
                    )

                    boundCamera = camera
                    videoCapture = capture
                    hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                    torchEnabled = false
                    cameraError = null
                } catch (e: Exception) {
                    cameraError = "Camera initialization failed: ${e.localizedMessage ?: "Hardware unavailable"}"
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    @SuppressLint("MissingPermission")
    fun beginActualRecording() {
        val capture = videoCapture
        if (capture == null) {
            scope.launch { snackbarHostState.showSnackbar("Camera is not ready for recording.") }
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val baseTitle = activeScript?.title
            ?.replace(Regex("[^A-Za-z0-9_\\- ]"), "")
            ?.trim()
            ?.replace(" ", "_")
            ?.take(20)
            .orEmpty()
        val fileName = if (baseTitle.isNotEmpty()) {
            "${VideoRepository.FILENAME_PREFIX}${baseTitle}_$timestamp.mp4"
        } else {
            "${VideoRepository.FILENAME_PREFIX}$timestamp.mp4"
        }

        try {
            val pending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, VideoRepository.RELATIVE_MOVIE_DIR)
                }
                val outputOptions = MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
                    .setContentValues(contentValues)
                    .build()
                capture.output.prepareRecording(context, outputOptions)
            } else {
                val outFile = File(videoRepository.getAppLocalVideosDir(), fileName)
                val outputOptions = FileOutputOptions.Builder(outFile).build()
                capture.output.prepareRecording(context, outputOptions)
            }

            val configuredPending = if (micEnabled && hasAudioPermission) {
                pending.withAudioEnabled()
            } else {
                pending
            }

            activeRecording = configuredPending.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        recordingDurationNanos = 0L
                    }

                    is VideoRecordEvent.Status -> {
                        recordingDurationNanos = event.recordingStats.recordedDurationNanos
                    }

                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        activeRecording = null
                        if (!event.hasError()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved locally: $fileName")
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Recording failed (code ${event.error}): ${event.cause?.localizedMessage ?: "Try lower resolution"}"
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            isRecording = false
            scope.launch {
                snackbarHostState.showSnackbar("Unable to start recording: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    // Recording countdown trigger
    LaunchedEffect(activeCountdown) {
        if (activeCountdown > 0) {
            delay(1000L)
            if (activeCountdown == 1) {
                activeCountdown = 0
                beginActualRecording()
            } else {
                activeCountdown -= 1
            }
        }
    }

    // Remote / Bluetooth keyboard shortcuts work for script scrolling even while recording
    LaunchedEffect(settings.remoteBindings) {
        val bindings = settings.remoteBindings
        RemoteKeyBus.events.collectLatest { keyCode ->
            when {
                keyCode == bindings.playPauseKeyCode || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (chunks.isNotEmpty()) isScrolling = !isScrolling
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
                    scope.launch { listState.scrollToItem(0, 0) }
                }

                keyCode == bindings.exitKeyCode -> {
                    runCatching { activeRecording?.stop() }
                    onNavigateBack()
                }
            }
        }
    }

    BackHandler {
        runCatching { activeRecording?.stop() }
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
    ) {
        if (!hasCameraPermission) {
            // Permission Request Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(84.dp)
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = TallyCrimson,
                        modifier = Modifier.padding(22.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Camera & Microphone Access",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Allow camera and microphone access to preview your lens and record local HD videos while reading your scrolling script. Videos are saved exclusively on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8C2D4),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("camera_grant_permission_button")
                ) {
                    Text("Grant Camera & Mic Permissions")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Studio")
                }
            }
        } else {
            // Live CameraX Preview Surface
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Camera Error Overlay (if device lacks hardware or init fails)
            cameraError?.let { errMsg ->
                Surface(
                    color = ObsidianCanvas.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = TallyCrimson,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Camera Unavailable",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB8C2D4),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            lensOption = if (lensOption == CameraLensOption.FRONT) {
                                CameraLensOption.REAR
                            } else {
                                CameraLensOption.FRONT
                            }
                        }) {
                            Text("Try Other Camera")
                        }
                    }
                }
            }

            // High-Contrast Teleprompter Script Overlay near top lens for natural eye contact
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.47f)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 58.dp, start = 10.dp, end = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = overlayOpacity))
                    .border(
                        width = 1.dp,
                        color = if (isScrolling) BroadcastAmber.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                if (chunks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No script loaded for overlay",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { showScriptPickerSheet = true }) {
                            Text("Choose Script")
                        }
                    }
                } else {
                    TeleprompterViewport(
                        chunks = chunks,
                        totalWordCount = totalWords,
                        listState = listState,
                        style = style,
                        isPlaying = isScrolling,
                        showReadingGuide = true,
                        guideVerticalBias = 0.34f,
                        transparentBackground = true,
                        onReachedEnd = { isScrolling = false },
                        onProgressChanged = { progressInfo = it }
                    )
                }
            }

            // Top Camera Toolbar (Back, Recording Timer, Script Picker, Flash, Switch Camera, Camera Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                        IconButton(
                            onClick = {
                                runCatching { activeRecording?.stop() }
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("camera_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Recording Timer / Tally Status Pill
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isRecording) TallyCrimson else Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color.White else TallyCrimson)
                            )
                            val totalSec = (recordingDurationNanos / 1_000_000_000L).toInt()
                            Text(
                                text = if (isRecording) {
                                    "REC %02d:%02d".format(totalSec / 60, totalSec % 60)
                                } else {
                                    "${resolutionOption.label.substringBefore(' ')} · ${fpsOption.fps}fps"
                                },
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Select Script
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                        IconButton(
                            onClick = { showScriptPickerSheet = true },
                            modifier = Modifier.testTag("camera_pick_script_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Select script", tint = Color.White)
                        }
                    }

                    // Flash / Torch
                    if (hasFlashUnit) {
                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                            IconButton(
                                onClick = {
                                    torchEnabled = !torchEnabled
                                    boundCamera?.cameraControl?.enableTorch(torchEnabled)
                                },
                                modifier = Modifier.testTag("camera_flash_button")
                            ) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Toggle flash",
                                    tint = if (torchEnabled) BroadcastAmber else Color.White
                                )
                            }
                        }
                    }

                    // Mic toggle
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                        IconButton(
                            onClick = {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                } else if (!isRecording) {
                                    micEnabled = !micEnabled
                                }
                            },
                            modifier = Modifier.testTag("camera_mic_button")
                        ) {
                            Icon(
                                imageVector = if (micEnabled && hasAudioPermission) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Toggle microphone",
                                tint = if (micEnabled && hasAudioPermission) Color.White else TallyCrimson
                            )
                        }
                    }

                    // Resolution / FPS config
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                        IconButton(
                            onClick = { if (!isRecording) showCameraConfigSheet = true },
                            modifier = Modifier.testTag("camera_quality_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Camera resolution and FPS", tint = Color.White)
                        }
                    }
                }
            }

            // Countdown Overlay
            AnimatedVisibility(
                visible = activeCountdown > 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.8f),
                    border = BorderStroke(3.dp, TallyCrimson),
                    modifier = Modifier.size(132.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = activeCountdown.toString(),
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 62.sp,
                            color = TallyCrimson
                        )
                    }
                }
            }

            // Bottom Dual-Deck Independent Controls: Row 1 = Script Scroll Controls | Row 2 = Video Recording Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Independent Script Scrolling Deck
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xDB0E121B),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause Script + Restart
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BroadcastAmber,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable { if (chunks.isNotEmpty()) isScrolling = !isScrolling }
                                    .testTag("camera_script_play_pause_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Scroll script",
                                        tint = Color(0xFF1A1100)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { scope.launch { listState.scrollToItem(0, 0) } },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("camera_script_restart_button")
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = "Restart script", tint = Color.White)
                            }
                        }

                        // Speed Control
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { style = style.copy(scrollSpeed = (style.scrollSpeed - 5).coerceAtLeast(1)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Slower", tint = Color.White)
                            }
                            Text(
                                text = "SPD ${style.scrollSpeed}",
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BroadcastAmber
                            )
                            IconButton(
                                onClick = { style = style.copy(scrollSpeed = (style.scrollSpeed + 5).coerceAtMost(300)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Faster", tint = Color.White)
                            }
                        }

                        // Mirror + Overlay Style
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val next = MirrorMode.entries[(style.mirrorMode.ordinal + 1) % MirrorMode.entries.size]
                                    style = style.copy(mirrorMode = next)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Flip,
                                    contentDescription = "Mirror Mode",
                                    tint = if (style.mirrorMode != MirrorMode.NORMAL) BroadcastAmber else Color.White
                                )
                            }
                            IconButton(
                                onClick = { showStyleSheet = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Text(
                                    text = "${style.fontSizeSp.roundToInt()}sp",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Independent Video Recording Deck
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video Gallery Shortcut
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.size(52.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (!isRecording) onOpenVideoGallery()
                            },
                            modifier = Modifier.testTag("camera_open_gallery_button")
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "Recorded videos", tint = Color.White)
                        }
                    }

                    // Primary Record / Stop Shutter Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Transparent else TallyCrimson)
                            .clickable {
                                if (isRecording) {
                                    activeRecording?.stop()
                                } else if (activeCountdown > 0) {
                                    activeCountdown = 0
                                } else if (countdownSeconds > 0) {
                                    activeCountdown = countdownSeconds
                                } else {
                                    beginActualRecording()
                                }
                            }
                            .testTag("camera_record_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TallyCrimson)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Start Recording",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Switch Front / Rear Camera
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.size(52.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (!isRecording) {
                                    lensOption = if (lensOption == CameraLensOption.FRONT) {
                                        CameraLensOption.REAR
                                    } else {
                                        CameraLensOption.FRONT
                                    }
                                }
                            },
                            modifier = Modifier.testTag("camera_switch_lens_button")
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch front or rear camera", tint = Color.White)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 148.dp)
        )
    }

    if (showStyleSheet) {
        TeleprompterStyleBottomSheet(
            style = style,
            countdownSeconds = countdownSeconds,
            showBackgroundColors = false,
            cameraOverlayOpacity = overlayOpacity,
            onStyleChange = { style = it },
            onCountdownChange = { countdownSeconds = it },
            onOverlayOpacityChange = { overlayOpacity = it },
            onDismiss = { showStyleSheet = false }
        )
    }

    if (showCameraConfigSheet) {
        ModalBottomSheet(onDismissRequest = { showCameraConfigSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Camera Recording Settings", style = MaterialTheme.typography.titleLarge)

                Text("Video Resolution", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoResolutionOption.entries.forEach { res ->
                        FilterChip(
                            selected = resolutionOption == res,
                            onClick = { resolutionOption = res },
                            label = { Text(res.label) }
                        )
                    }
                }

                Text("Target Frame Rate (where supported)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoFpsOption.entries.forEach { fps ->
                        FilterChip(
                            selected = fpsOption == fps,
                            onClick = { fpsOption = fps },
                            label = { Text(fps.label) }
                        )
                    }
                }

                Text("Recording Countdown", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 3, 5, 10).forEach { sec ->
                        FilterChip(
                            selected = countdownSeconds == sec,
                            onClick = { countdownSeconds = sec },
                            label = { Text(if (sec == 0) "Off" else "${sec}s") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showScriptPickerSheet) {
        ModalBottomSheet(onDismissRequest = { showScriptPickerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Script for Camera Overlay", style = MaterialTheme.typography.titleLarge)
                if (allScripts.isEmpty()) {
                    Text(
                        text = "No saved scripts yet. Create a script first or paste text in Quick Teleprompter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(320.dp)
                    ) {
                        items(allScripts, key = { it.id }) { item ->
                            val selected = item.id == activeScriptId
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        activeScriptId = item.id
                                        isScrolling = false
                                        scope.launch { listState.scrollToItem(0, 0) }
                                        showScriptPickerSheet = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.wordCount()} words · ${item.detectedScriptTag()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
