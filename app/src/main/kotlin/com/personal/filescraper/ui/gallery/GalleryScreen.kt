package com.personal.filescraper.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
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

@Composable
fun GalleryScreen(viewModel: GalleryViewModel) {
    val files by viewModel.uiState.collectAsStateWithLifecycle()
    var previewPath by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    if (files.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No archived files yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(files, key = { it.id }) { file ->
            ArchivedFileCard(
                file = file,
                onClick = {
                    if (file.fileType == FileType.IMAGE) {
                        previewPath = file.archivedPath
                    } else {
                        FileUtils.openWithDefaultApp(context, file.archivedPath)
                    }
                }
            )
        }
    }

    previewPath?.let { path ->
        ImagePreviewDialog(path = path, onDismiss = { previewPath = null })
    }
}

@Composable
private fun ArchivedFileCard(file: ArchivedFileUi, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
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

private fun formatRemaining(millis: Long): String {
    if (millis <= 0) return "Expiring…"
    val hours = millis / 3_600_000L
    val minutes = (millis % 3_600_000L) / 60_000L
    return if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
}
