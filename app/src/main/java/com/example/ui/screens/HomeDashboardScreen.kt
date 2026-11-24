package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ScriptEntity
import com.example.ui.components.OfflinePrivacyPill
import com.example.ui.components.formatDurationShort
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ScriptUnicodeFontFamily
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.TallyCrimson
import kotlin.math.roundToInt

@Composable
fun HomeDashboardScreen(
    recentScripts: List<ScriptEntity>,
    totalScriptsCount: Int,
    quickScriptText: String,
    onUpdateQuickScript: (String) -> Unit,
    onCreateNewScript: () -> Unit,
    onImportDocument: () -> Unit,
    onOpenMyScripts: () -> Unit,
    onOpenScriptEditor: (Long) -> Unit,
    onStartTeleprompter: (Long) -> Unit,
    onStartQuickTeleprompter: () -> Unit,
    onSaveQuickAsScript: (String, String) -> Unit,
    onOpenCameraTeleprompter: (Long) -> Unit,
    onOpenVideoGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuickPromptDialog by remember { mutableStateOf(false) }
    var showRecentSectionHighlighted by remember { mutableStateOf(false) }

    val primaryScript = recentScripts.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Brand Bar
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SMART TELEPROMPTER PRO",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OfflinePrivacyPill()
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("dashboard_top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings"
                        )
                    }
                }
            }
        }

        // Studio Hero Card with Prominent START Button
        item(key = "hero_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianCanvas)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero_1795518309280),
                    contentDescription = "Broadcast teleprompter studio banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ObsidianCanvas.copy(alpha = 0.94f),
                                    ObsidianCanvas.copy(alpha = 0.72f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BroadcastAmber)
                        )
                        Text(
                            text = if (primaryScript != null) "READY TO PROMPT" else "STUDIO READY",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastAmber
                        )
                    }

                    Text(
                        text = primaryScript?.title ?: "Paste or Write Any Script & Roll",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFF4F6FA),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (primaryScript != null) {
                            "${primaryScript.wordCount()} words · Est. ${formatDurationShort(primaryScript.estimatedDurationSeconds())} · ${(primaryScript.lastReadingProgress * 100).roundToInt()}% read"
                        } else {
                            "Smooth 1–300 speed engine · Bengali, Hindi & English · Local CameraX HD recording"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8C2D4)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (primaryScript != null) {
                                    onStartTeleprompter(primaryScript.id)
                                } else {
                                    showQuickPromptDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BroadcastAmber,
                                contentColor = Color(0xFF1A1100)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("dashboard_start_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                onOpenCameraTeleprompter(primaryScript?.id ?: -1L)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E2636),
                                contentColor = Color(0xFFF4F6FA)
                            ),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("dashboard_hero_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Open Camera Teleprompter",
                                tint = TallyCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REC")
                        }
                    }
                }
            }
        }

        // Studio Workspace Grid (All 8 Required Dashboard Modules)
        item(key = "workspace_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Studio Controls",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$totalScriptsCount saved script${if (totalScriptsCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "workspace_grid") {
            val actions = listOf(
                DashboardTile(
                    title = "Create New Script",
                    subtitle = "Write or paste text",
                    icon = Icons.Default.Add,
                    accent = BroadcastAmber,
                    testTag = "tile_create_script",
                    onClick = onCreateNewScript
                ),
                DashboardTile(
                    title = "Import Document",
                    subtitle = "TXT · PDF · DOCX",
                    icon = Icons.Default.UploadFile,
                    accent = StudioTeal,
                    testTag = "tile_import_document",
                    onClick = onImportDocument
                ),
                DashboardTile(
                    title = "My Scripts",
                    subtitle = "$totalScriptsCount in library",
                    icon = Icons.Default.Description,
                    accent = MaterialTheme.colorScheme.primary,
                    testTag = "tile_my_scripts",
                    onClick = onOpenMyScripts
                ),
                DashboardTile(
                    title = "Recent Scripts",
                    subtitle = "${recentScripts.size} recent",
                    icon = Icons.Default.History,
                    accent = StudioTeal,
                    testTag = "tile_recent_scripts",
                    onClick = {
                        if (recentScripts.isEmpty()) {
                            onCreateNewScript()
                        } else {
                            showRecentSectionHighlighted = !showRecentSectionHighlighted
                        }
                    }
                ),
                DashboardTile(
                    title = "Quick Teleprompter",
                    subtitle = "Paste & scroll now",
                    icon = Icons.Default.Bolt,
                    accent = BroadcastAmber,
                    testTag = "tile_quick_teleprompter",
                    onClick = { showQuickPromptDialog = true }
                ),
                DashboardTile(
                    title = "Camera Teleprompter",
                    subtitle = "Live preview & REC",
                    icon = Icons.Default.Videocam,
                    accent = TallyCrimson,
                    testTag = "tile_camera_teleprompter",
                    onClick = { onOpenCameraTeleprompter(primaryScript?.id ?: -1L) }
                ),
                DashboardTile(
                    title = "Recorded Videos",
                    subtitle = "Local MP4 gallery",
                    icon = Icons.Default.VideoLibrary,
                    accent = StudioTeal,
                    testTag = "tile_recorded_videos",
                    onClick = onOpenVideoGallery
                ),
                DashboardTile(
                    title = "Settings",
                    subtitle = "Speed, remote, theme",
                    icon = Icons.Default.Settings,
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "tile_settings",
                    onClick = onOpenSettings
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { tile ->
                            DashboardActionCard(
                                tile = tile,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Recent Scripts Section
        item(key = "recent_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Scripts",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (showRecentSectionHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onOpenMyScripts) {
                    Text("View All")
                }
            }
        }

        if (recentScripts.isEmpty()) {
            item(key = "recent_empty") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No scripts saved yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Create a new script, paste text into Quick Teleprompter, or import a local TXT, PDF, or DOCX file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onCreateNewScript,
                                modifier = Modifier.testTag("empty_recent_create_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("New Script")
                            }
                            OutlinedButton(
                                onClick = { showQuickPromptDialog = true },
                                modifier = Modifier.testTag("empty_recent_quick_button")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Quick Paste")
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "recent_carousel") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentScripts, key = { it.id }) { script ->
                        RecentScriptCard(
                            script = script,
                            onRead = { onStartTeleprompter(script.id) },
                            onCamera = { onOpenCameraTeleprompter(script.id) },
                            onEdit = { onOpenScriptEditor(script.id) }
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showQuickPromptDialog) {
        QuickTeleprompterDialog(
            initialText = quickScriptText,
            onTextChange = onUpdateQuickScript,
            onLaunchNow = {
                showQuickPromptDialog = false
                onStartQuickTeleprompter()
            },
            onSaveAndLaunch = { title, text ->
                showQuickPromptDialog = false
                onSaveQuickAsScript(title, text)
            },
            onDismiss = { showQuickPromptDialog = false }
        )
    }
}

private data class DashboardTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun DashboardActionCard(
    tile: DashboardTile,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = tile.onClick)
            .testTag(tile.testTag),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tile.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.title,
                    tint = tile.accent,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tile.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentScriptCard(
    script: ScriptEntity,
    onRead: () -> Unit,
    onCamera: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onRead)
            .testTag("recent_script_card_${script.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = script.detectedScriptTag(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = formatRelativeDate(script.modifiedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = script.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = script.content.ifBlank { "Empty script" },
                fontFamily = ScriptUnicodeFontFamily,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            LinearProgressIndicator(
                progress = { script.lastReadingProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${script.wordCount()}w · ${formatDurationShort(script.estimatedDurationSeconds())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit script",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onCamera, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera teleprompter",
                            tint = TallyCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onRead, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start reading",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTeleprompterDialog(
    initialText: String,
    onTextChange: (String) -> Unit,
    onLaunchNow: () -> Unit,
    onSaveAndLaunch: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var text by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Teleprompter")
                TextButton(
                    onClick = {
                        val pasted = clipboardManager.getText()?.text.orEmpty()
                        if (pasted.isNotBlank()) {
                            text = pasted
                            onTextChange(pasted)
                        }
                    },
                    modifier = Modifier.testTag("quick_paste_clipboard_button")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste or type any script in English, বাংলা, or हिन्दी and press Start to scroll immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onTextChange(it)
                    },
                    placeholder = { Text("Paste your script here…") },
                    minLines = 6,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = ScriptUnicodeFontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_script_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTextChange(text)
                    onLaunchNow()
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("quick_start_now_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start")
            }
        },
        dismissButton = {
            Row {
                if (text.isNotBlank()) {
                    TextButton(
                        onClick = {
                            val title = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(36)?.trim()
                                ?: "Quick Script"
                            onSaveAndLaunch(title, text)
                        },
                        modifier = Modifier.testTag("quick_save_and_start_button")
                    ) {
                        Text("Save & Start")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
