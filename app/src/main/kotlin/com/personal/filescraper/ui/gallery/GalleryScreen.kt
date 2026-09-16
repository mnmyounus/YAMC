@file:OptIn(ExperimentalMaterial3Api::class)

package com.personal.filescraper.ui.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.personal.filescraper.data.model.FileType
import com.personal.filescraper.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GalleryScreen(viewModel: GalleryViewModel) {
    val files by viewModel.uiState.collectAsStateWithLifecycle()
    var previewPath by remember { mutableStateOf<String?>(null) }
    var videoPath by remember { mutableStateOf<String?>(null) }
    var pdfPath by remember { mutableStateOf<String?>(null) }
    var textPath by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<ArchivedFileUi?>(null) }
    var deleteTarget by remember { mutableStateOf<ArchivedFileUi?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            scope.launch {
                val count = viewModel.exportAll(context, uri)
                snackbarHostState.showSnackbar(if (count > 0) "Exported $count file(s)" else "Nothing to export")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                actions = {
                    IconButton(onClick = { exportLauncher.launch(null) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export all before deletion")
                    }
                }
            )
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No archived files yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(files, key = { it.id }) { file ->
                    ArchivedFileCard(
                        file = file,
                        onOpen = {
                            val f = File(file.archivedPath)
                            when {
                                file.fileType == FileType.IMAGE -> previewPath = file.archivedPath
                                file.fileType == FileType.VIDEO -> videoPath = file.archivedPath
                                FileUtils.isPdf(f) -> pdfPath = file.archivedPath
                                FileUtils.isPlainText(f) -> textPath = file.archivedPath
                                else -> FileUtils.openWithDefaultApp(context, file.archivedPath)
                            }
                        },
                        onRename = { renameTarget = file },
                        onDelete = { deleteTarget = file },
                        onShare = { FileUtils.shareFile(context, file.archivedPath) }
                    )
                }
            }
        }
    }

    previewPath?.let { path -> ImagePreviewDialog(path = path, onDismiss = { previewPath = null }) }
    videoPath?.let { path -> VideoPlayerDialog(path = path, onDismiss = { videoPath = null }) }
    pdfPath?.let { path -> PdfViewerDialog(path = path, onDismiss = { pdfPath = null }) }
    textPath?.let { path -> TextViewerDialog(path = path, onDismiss = { textPath = null }) }

    renameTarget?.let { file ->
        RenameFileDialog(
            currentName = file.fileName,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                scope.launch {
                    val ok = viewModel.renameFile(file.id, newName)
                    if (!ok) snackbarHostState.showSnackbar("Couldn't rename - a file with that name already exists")
                    renameTarget = null
                }
            }
        )
    }

    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this file?") },
            text = { Text("\"${file.fileName}\" will be permanently deleted now, ahead of its normal schedule.") },
            confirmButton = {
                TextButton(
                    onClick = { scope.launch { viewModel.deleteFile(file.id); deleteTarget = null } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ArchivedFileCard(
    file: ArchivedFileUi,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.clickable(onClick = onOpen)) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (file.fileType == FileType.IMAGE) {
                    AsyncImage(
                        model = file.archivedPath,
                        contentDescription = file.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (file.fileType == FileType.VIDEO) Icons.Default.Videocam else Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = { showMenu = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(file.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    formatRemaining(file.timeRemainingMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (file.timeRemainingMillis < 3_600_000L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = path,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun RenameFileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename file") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatRemaining(millis: Long): String {
    if (millis <= 0) return "Expiring…"
    val hours = millis / 3_600_000L
    val minutes = (millis % 3_600_000L) / 60_000L
    return if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
}
