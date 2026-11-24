package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MirrorMode
import com.example.data.model.PrompterAlignment
import com.example.data.model.ScrollDirection
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.ScriptUnicodeFontFamily
import kotlin.math.roundToInt

/**
 * Pure chunking helper that splits arbitrarily large Unicode scripts (Bengali, Hindi, English)
 * into memory-efficient LazyColumn items while preserving paragraph breaks and readability.
 */
object ScriptChunker {
    private const val MAX_CHUNK_CHARS = 520

    fun splitIntoParagraphChunks(rawText: String): List<String> {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isEmpty()) return emptyList()

        val rawParagraphs = normalized.split(Regex("\n+"))
        val chunks = ArrayList<String>(rawParagraphs.size)

        for (para in rawParagraphs) {
            val trimmed = para.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.length <= MAX_CHUNK_CHARS) {
                chunks.add(trimmed)
            } else {
                // Split very long paragraphs gracefully at whitespace boundaries
                var start = 0
                while (start < trimmed.length) {
                    val tentativeEnd = (start + MAX_CHUNK_CHARS).coerceAtMost(trimmed.length)
                    if (tentativeEnd == trimmed.length) {
                        chunks.add(trimmed.substring(start).trim())
                        break
                    }
                    val lastSpace = trimmed.lastIndexOf(' ', tentativeEnd)
                    val cut = if (lastSpace > start + MAX_CHUNK_CHARS / 2) lastSpace else tentativeEnd
                    chunks.add(trimmed.substring(start, cut).trim())
                    start = if (cut < trimmed.length && trimmed[cut] == ' ') cut + 1 else cut
                }
            }
        }
        return chunks
    }

    /**
     * Converts teleprompter speed (1..300) into smooth density-independent pixels per second.
     * Calibrated for fine control at low reading speeds and rapid scanning up to 300.
     */
    fun speedToDpPerSecond(speed: Int): Float {
        val clamped = speed.coerceIn(1, 300).toFloat()
        return 6f + (clamped * 0.85f) + (clamped * clamped * 0.00075f)
    }
}

data class TeleprompterStyleConfig(
    val fontSizeSp: Float = 36f,
    val scrollSpeed: Int = 65,
    val lineSpacingMultiplier: Float = 1.45f,
    val paragraphSpacingDp: Int = 18,
    val horizontalMarginDp: Int = 20,
    val readingAreaWidthFraction: Float = 1.0f,
    val alignment: PrompterAlignment = PrompterAlignment.LEFT,
    val textColor: Color = Color.White,
    val backgroundColor: Color = Color.Black,
    val mirrorMode: MirrorMode = MirrorMode.NORMAL,
    val scrollDirection: ScrollDirection = ScrollDirection.UP
)

data class TeleprompterProgressInfo(
    val progress: Float,
    val currentChunkIndex: Int,
    val totalChunks: Int,
    val estimatedWordsRemaining: Int,
    val estimatedSecondsRemaining: Int
)

/**
 * Core high-precision vertical teleprompter scrolling viewport.
 * - Uses withFrameNanos delta-time loop + sub-pixel accumulator (zero jitter, zero jumps)
 * - Uses LazyColumn chunking to handle 100,000+ word scripts smoothly with constant memory
 * - Preserves exact position when paused and yields instantly to manual finger scrolling
 * - Applies MirrorMode strictly visually via graphicsLayer without modifying saved text
 */
@Composable
fun TeleprompterViewport(
    chunks: List<String>,
    totalWordCount: Int,
    listState: LazyListState,
    style: TeleprompterStyleConfig,
    isPlaying: Boolean,
    showReadingGuide: Boolean = true,
    guideVerticalBias: Float = 0.42f,
    transparentBackground: Boolean = false,
    onReachedEnd: () -> Unit,
    onProgressChanged: (TeleprompterProgressInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Compute progress & remaining estimates reactively without excess recomposition
    val progressInfo by remember(chunks.size, totalWordCount, style.scrollSpeed) {
        derivedStateOf {
            if (chunks.isEmpty()) {
                TeleprompterProgressInfo(0f, 0, 0, 0, 0)
            } else {
                // Index 0 is top spacer, chunks occupy indices 1..chunks.size
                val rawIndex = (listState.firstVisibleItemIndex - 1).coerceIn(0, chunks.size)
                val layoutInfo = listState.layoutInfo
                val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()
                val intraFraction = if (firstVisible != null && firstVisible.size > 0) {
                    (listState.firstVisibleItemScrollOffset.toFloat() / firstVisible.size.toFloat())
                        .coerceIn(0f, 1f)
                } else 0f

                val effectivePos = (rawIndex + intraFraction).coerceIn(0f, chunks.size.toFloat())
                val progress = if (chunks.size <= 1) {
                    if (listState.canScrollForward) intraFraction else 1f
                } else {
                    (effectivePos / chunks.size.toFloat()).coerceIn(0f, 1f)
                }

                val remainingWords = ((1f - progress) * totalWordCount).roundToInt().coerceAtLeast(0)
                // Estimate WPM dynamically scaled loosely by speed
                val effectiveWpm = (60f + style.scrollSpeed * 1.15f).coerceIn(70f, 320f)
                val remainingSec = if (remainingWords == 0) 0 else ((remainingWords * 60f) / effectiveWpm).roundToInt().coerceAtLeast(1)

                TeleprompterProgressInfo(
                    progress = progress,
                    currentChunkIndex = rawIndex.coerceAtMost((chunks.size - 1).coerceAtLeast(0)),
                    totalChunks = chunks.size,
                    estimatedWordsRemaining = remainingWords,
                    estimatedSecondsRemaining = remainingSec
                )
            }
        }
    }

    LaunchedEffect(progressInfo) {
        onProgressChanged(progressInfo)
    }

    // Precision frame-synced scrolling engine
    LaunchedEffect(isPlaying, style.scrollSpeed, style.scrollDirection, chunks.size) {
        if (!isPlaying || chunks.isEmpty()) return@LaunchedEffect

        var lastFrameNanos = 0L
        var subPixelCarry = 0f

        while (true) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameNanos
                continue
            }

            val dtSeconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrameNanos = frameNanos

            // Yield smoothly while user is dragging manually
            if (listState.isScrollInProgress) {
                subPixelCarry = 0f
                continue
            }

            val pxPerSecond = with(density) {
                ScriptChunker.speedToDpPerSecond(style.scrollSpeed).dp.toPx()
            }
            val directionSign = if (style.scrollDirection == ScrollDirection.UP) 1f else -1f
            val desiredDelta = (pxPerSecond * dtSeconds * directionSign) + subPixelCarry

            // Avoid scrolling if we already hit boundary
            if (directionSign > 0f && !listState.canScrollForward) {
                onReachedEnd()
                break
            }
            if (directionSign < 0f && !listState.canScrollBackward) {
                onReachedEnd()
                break
            }

            val consumed = listState.scrollBy(desiredDelta)
            subPixelCarry = desiredDelta - consumed

            // If movement completely stalled at boundary
            if (kotlin.math.abs(consumed) < 0.001f && kotlin.math.abs(desiredDelta) > 1.5f) {
                if ((directionSign > 0f && !listState.canScrollForward) ||
                    (directionSign < 0f && !listState.canScrollBackward)
                ) {
                    onReachedEnd()
                    break
                }
            }
        }
    }

    val scaleX = when (style.mirrorMode) {
        MirrorMode.HORIZONTAL, MirrorMode.BOTH -> -1f
        else -> 1f
    }
    val scaleY = when (style.mirrorMode) {
        MirrorMode.VERTICAL, MirrorMode.BOTH -> -1f
        else -> 1f
    }

    val textAlign = when (style.alignment) {
        PrompterAlignment.LEFT -> TextAlign.Start
        PrompterAlignment.CENTER -> TextAlign.Center
        PrompterAlignment.JUSTIFIED -> TextAlign.Justify
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (transparentBackground) Modifier else Modifier.background(style.backgroundColor)
            )
    ) {
        val viewportHeight = maxHeight
        val leadSpacerHeight = (viewportHeight * guideVerticalBias).coerceAtLeast(80.dp)
        val trailSpacerHeight = (viewportHeight * (1f - guideVerticalBias + 0.25f)).coerceAtLeast(160.dp)

        // Mirrored script canvas (transformations never alter saved data)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                },
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(style.readingAreaWidthFraction.coerceIn(0.5f, 1.0f)),
                contentPadding = PaddingValues(horizontal = style.horizontalMarginDp.dp)
            ) {
                item(key = "lead_spacer") {
                    Spacer(modifier = Modifier.height(leadSpacerHeight))
                }

                itemsIndexed(
                    items = chunks,
                    key = { index, _ -> "chunk_$index" }
                ) { _, paragraph ->
                    Text(
                        text = paragraph,
                        color = style.textColor,
                        textAlign = textAlign,
                        style = TextStyle(
                            fontFamily = ScriptUnicodeFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = style.fontSizeSp.sp,
                            lineHeight = (style.fontSizeSp * style.lineSpacingMultiplier).sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = style.paragraphSpacingDp.dp)
                    )
                }

                item(key = "trail_spacer") {
                    Spacer(modifier = Modifier.height(trailSpacerHeight))
                }
            }
        }

        // Top and bottom soft edge vignettes for studio focus (non-transparent reader mode)
        if (!transparentBackground) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(style.backgroundColor, Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, style.backgroundColor)
                        )
                    )
            )
        }

        // Large, prominent central reading guide
        if (showReadingGuide) {
            ReadingGuideOverlay(
                verticalBias = guideVerticalBias,
                guideBandHeightDp = (style.fontSizeSp * style.lineSpacingMultiplier * 1.45f).coerceIn(52f, 132f),
                accentColor = BroadcastAmber,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReadingGuideOverlay(
    verticalBias: Float,
    guideBandHeightDp: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerY = size.height * verticalBias
        val bandHalf = (guideBandHeightDp.dp.toPx()) / 2f
        val topY = (centerY - bandHalf).coerceAtLeast(0f)
        val bottomY = (centerY + bandHalf).coerceAtMost(size.height)

        // Subtle focus highlight band
        drawRect(
            color = accentColor.copy(alpha = 0.08f),
            topLeft = Offset(0f, topY),
            size = androidx.compose.ui.geometry.Size(size.width, bottomY - topY)
        )

        // Crisp top and bottom hairline borders
        drawLine(
            color = accentColor.copy(alpha = 0.38f),
            start = Offset(0f, topY),
            end = Offset(size.width, topY),
            strokeWidth = 1.25.dp.toPx()
        )
        drawLine(
            color = accentColor.copy(alpha = 0.38f),
            start = Offset(0f, bottomY),
            end = Offset(size.width, bottomY),
            strokeWidth = 1.25.dp.toPx()
        )

        // Left bold pointer triangle (large & unmistakable eye-line anchor)
        val triW = 14.dp.toPx()
        val triH = 18.dp.toPx()
        val leftPath = Path().apply {
            moveTo(0f, centerY - triH / 2f)
            lineTo(triW, centerY)
            lineTo(0f, centerY + triH / 2f)
            close()
        }
        drawPath(path = leftPath, color = accentColor)

        // Right bold pointer triangle
        val rightPath = Path().apply {
            moveTo(size.width, centerY - triH / 2f)
            lineTo(size.width - triW, centerY)
            lineTo(size.width, centerY + triH / 2f)
            close()
        }
        drawPath(path = rightPath, color = accentColor)
    }
}
