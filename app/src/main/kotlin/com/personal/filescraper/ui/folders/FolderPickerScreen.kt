@file:OptIn(ExperimentalMaterial3Api::class)

package com.personal.filescraper.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.filescraper.data.db.WatchedFolderEntity
import com.personal.filescraper.ui.components.DirectoryBrowserDialog

@Composable
fun FolderPickerScreen(viewModel: FolderPickerViewModel) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var showBrowser by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<WatchedFolderEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBrowser = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add folder")
            }
        }
    ) { padding ->
        if (folders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No folders yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text(
                    "Swipe a folder left to stop watching it (including default ones). Tap the pencil to rename it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(folders, key = { it.path }) { folder ->
                        SwipeToDeleteFolderRow(
                            folder = folder,
                            onDelete = { viewModel.removeFolder(folder.path, context) },
                            onEditRequest = { renameTarget = folder }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showBrowser) {
        DirectoryBrowserDialog(
            onDismiss = { showBrowser = false },
            onFolderSelected = { path ->
                viewModel.addFolder(path, context)
                showBrowser = false
            }
        )
    }

    renameTarget?.let { folder ->
        RenameFolderDialog(
            currentName = folder.displayName,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameFolder(folder.path, newName)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun SwipeToDeleteFolderRow(
    folder: WatchedFolderEntity,
    onDelete: () -> Unit,
    onEditRequest: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Stop watching this folder",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        ListItem(
            headlineContent = { Text(folder.displayName) },
            supportingContent = {
                Text(if (folder.isDefault) "${folder.path} · Default" else folder.path, maxLines = 1)
            },
            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
            trailingContent = {
                IconButton(onClick = onEditRequest) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
private fun RenameFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename folder") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
