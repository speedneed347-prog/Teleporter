package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSettings
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RenameItemDialog
import com.example.ui.theme.ScriptUnicodeFontFamily
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.TallyCrimson
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.SaveStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptId: Long,
    settings: AppSettings,
    editorViewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onOpenImportDocument: () -> Unit,
    onStartTeleprompter: (Long) -> Unit,
    onStartCameraTeleprompter: (Long) -> Unit
) {
    val uiState by editorViewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scriptId) {
        editorViewModel.loadScript(scriptId, settings)
    }

    DisposableEffect(Unit) {
        onDispose {
            editorViewModel.saveNow()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            editorViewModel.saveNow()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (uiState.scriptId > 0L) "Edit Script" else "New Script",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val statusLabel = when (uiState.saveStatus) {
                            SaveStatus.SAVING -> "Auto-saving…"
                            SaveStatus.SAVED -> "Saved locally"
                            SaveStatus.ERROR -> "Save error"
                            SaveStatus.IDLE -> "Ready"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (uiState.saveStatus == SaveStatus.SAVED) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StudioTeal,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val pasted = clipboardManager.getText()?.text.orEmpty()
                            if (pasted.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Clipboard is empty") }
                            } else {
                                editorViewModel.appendOrPasteText(pasted)
                                scope.launch { snackbarHostState.showSnackbar("Pasted text from clipboard") }
                            }
                        },
                        modifier = Modifier.testTag("editor_paste_button")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                    }
                    IconButton(
                        onClick = onOpenImportDocument,
                        modifier = Modifier.testTag("editor_import_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import TXT, PDF, or DOCX")
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("editor_more_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Script actions")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save Now") },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    editorViewModel.saveNow {
                                        scope.launch { snackbarHostState.showSnackbar("Script saved locally") }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename Script") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate Script") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                enabled = uiState.content.isNotBlank() || uiState.title.isNotBlank(),
                                onClick = {
                                    showMenu = false
                                    editorViewModel.duplicateCurrentScript {
                                        scope.launch { snackbarHostState.showSnackbar("Duplicate created in My Scripts") }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Script", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                enabled = uiState.scriptId > 0L,
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (uiState.content.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Enter or paste script text before opening Camera Teleprompter.")
                                }
                            } else {
                                editorViewModel.saveNow { savedId ->
                                    onStartCameraTeleprompter(savedId)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("editor_camera_button")
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Record with Camera",
                            tint = TallyCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }

                    Button(
                        onClick = {
                            if (uiState.content.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("This script is empty. Write or paste text before starting.")
                                }
                            } else {
                                editorViewModel.saveNow { savedId ->
                                    onStartTeleprompter(savedId)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("editor_start_teleprompter_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start Teleprompter")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Telemetry strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.wordCount} words · ${uiState.charCount} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Est. read ${uiState.estimatedMinutesText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = editorViewModel::onTitleChange,
                    label = { Text("Script Title") },
                    placeholder = { Text("e.g., Product Launch Keynote / বাংলা সংবাদ") },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = ScriptUnicodeFontFamily,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )

                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = editorViewModel::onContentChange,
                    label = { Text("Script Content (English · বাংলা · हिन्दी)") },
                    placeholder = { Text("Write, paste, or import your script here. All Bengali, Hindi, and Unicode characters are preserved locally.") },
                    textStyle = TextStyle(
                        fontFamily = ScriptUnicodeFontFamily,
                        fontSize = 16.sp,
                        lineHeight = 25.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("editor_content_input")
                )
            }
        }
    }

    if (showRenameDialog) {
        RenameItemDialog(
            dialogTitle = "Rename Script",
            initialValue = uiState.title,
            label = "New Script Title",
            onConfirm = { newTitle ->
                editorViewModel.onTitleChange(newTitle)
                editorViewModel.saveNow()
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Script?",
            message = "This will permanently remove \"${uiState.title.ifBlank { "this script" }}\" from your local device library.",
            onConfirm = {
                showDeleteDialog = false
                editorViewModel.deleteCurrentScript(onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
