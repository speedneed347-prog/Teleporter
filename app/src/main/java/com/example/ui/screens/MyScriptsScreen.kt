package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ScriptEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RenameItemDialog
import com.example.ui.components.StudioEmptyState
import com.example.ui.components.formatDurationShort
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.ScriptUnicodeFontFamily
import com.example.ui.theme.TallyCrimson
import com.example.viewmodel.ScriptSortOrder
import kotlin.math.roundToInt

@Composable
fun MyScriptsScreen(
    scripts: List<ScriptEntity>,
    searchQuery: String,
    sortOrder: ScriptSortOrder,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (ScriptSortOrder) -> Unit,
    onCreateNewScript: () -> Unit,
    onImportDocument: () -> Unit,
    onEditScript: (Long) -> Unit,
    onStartTeleprompter: (Long) -> Unit,
    onStartCameraTeleprompter: (Long) -> Unit,
    onRenameScript: (Long, String) -> Unit,
    onDuplicateScript: (ScriptEntity) -> Unit,
    onDeleteScript: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var scriptToRename by remember { mutableStateOf<ScriptEntity?>(null) }
    var scriptToDelete by remember { mutableStateOf<ScriptEntity?>(null) }

    val sortedScripts = remember(scripts, sortOrder) {
        when (sortOrder) {
            ScriptSortOrder.MODIFIED -> scripts.sortedByDescending { it.modifiedAt }
            ScriptSortOrder.CREATED -> scripts.sortedByDescending { it.createdAt }
            ScriptSortOrder.TITLE -> scripts.sortedBy { it.title.lowercase() }
        }
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
                        text = "My Scripts",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${sortedScripts.size} script${if (sortedScripts.size == 1) "" else "s"} stored locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onImportDocument,
                    modifier = Modifier.testTag("scripts_import_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Import document")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search scripts by title or text…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scripts_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScriptSortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = sortOrder == order,
                        onClick = { onSortOrderChange(order) },
                        label = { Text(order.label, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (sortedScripts.isEmpty()) {
                StudioEmptyState(
                    icon = Icons.Default.Description,
                    title = if (searchQuery.isBlank()) "Your Script Library is Empty" else "No Matching Scripts",
                    subtitle = if (searchQuery.isBlank()) {
                        "Create your first script or import a local TXT, PDF, or DOCX file to start reading smoothly."
                    } else {
                        "Try a different keyword or clear your search filter."
                    },
                    primaryActionLabel = if (searchQuery.isBlank()) "Create New Script" else "Clear Search",
                    onPrimaryAction = {
                        if (searchQuery.isBlank()) onCreateNewScript() else onSearchQueryChange("")
                    },
                    secondaryActionLabel = if (searchQuery.isBlank()) "Import TXT / PDF / DOCX" else null,
                    onSecondaryAction = if (searchQuery.isBlank()) onImportDocument else null,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedScripts, key = { it.id }) { script ->
                        ScriptLibraryCard(
                            script = script,
                            onEdit = { onEditScript(script.id) },
                            onRead = { onStartTeleprompter(script.id) },
                            onCamera = { onStartCameraTeleprompter(script.id) },
                            onRename = { scriptToRename = script },
                            onDuplicate = { onDuplicateScript(script) },
                            onDelete = { scriptToDelete = script }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateNewScript,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("scripts_fab_create")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create New Script")
        }
    }

    scriptToRename?.let { target ->
        RenameItemDialog(
            dialogTitle = "Rename Script",
            initialValue = target.title,
            label = "Script Title",
            onConfirm = { newTitle ->
                onRenameScript(target.id, newTitle)
                scriptToRename = null
            },
            onDismiss = { scriptToRename = null }
        )
    }

    scriptToDelete?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Script?",
            message = "Delete \"${target.title}\" permanently from your local library?",
            onConfirm = {
                onDeleteScript(target.id)
                scriptToDelete = null
            },
            onDismiss = { scriptToDelete = null }
        )
    }
}

@Composable
private fun ScriptLibraryCard(
    script: ScriptEntity,
    onEdit: () -> Unit,
    onRead: () -> Unit,
    onCamera: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onEdit)
            .testTag("script_library_card_${script.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
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
                        text = script.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Script options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Script") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = { menuExpanded = false; onDuplicate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }

            Text(
                text = script.content.ifBlank { "No text yet — tap to edit" },
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
                    text = "${script.wordCount()}w · ${formatDurationShort(script.estimatedDurationSeconds())} · ${(script.lastReadingProgress * 100).roundToInt()}% · ${formatRelativeDate(script.modifiedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCamera,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("script_card_camera_${script.id}")
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Camera Teleprompter",
                            tint = TallyCrimson
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onRead)
                            .testTag("script_card_read_${script.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Start Teleprompter",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Read",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
