@file:Suppress("DEPRECATION")

package com.personal.filescraper.ui.components

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class StorageRoot(val label: String, val directory: File)

@Composable
fun DirectoryBrowserDialog(
    onDismiss: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val roots = remember { listStorageRoots(context) }
    var currentDir by remember { mutableStateOf(roots.first().directory) }
    var entries by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Listing a directory is real disk I/O. Doing it synchronously inside a Composable
    // (as this used to) blocks the UI thread on every tap - that was the cause of the
    // reported lag/freeze while browsing. This runs it on a background dispatcher instead.
    LaunchedEffect(currentDir) {
        isLoading = true
        entries = withContext(Dispatchers.IO) {
            try {
                currentDir.listFiles()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Column(Modifier.padding(16.dp)) {
                Text(currentDir.absolutePath, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(8.dp))

                // Only shown when there's more than one volume (e.g. an SD card) to jump
                // between - this is what makes browsing reach the whole device, not just
                // internal storage, similar to a general-purpose file manager.
                if (roots.size > 1) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roots.forEach { root ->
                            AssistChip(onClick = { currentDir = root.directory }, label = { Text(root.label) })
                        }
                    }
                }

                currentDir.parentFile?.let { parent ->
                    TextButton(onClick = { currentDir = parent }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Up")
                    }
                }

                Box(Modifier.weight(1f)) {
                    when {
                        isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        entries.isEmpty() -> Text(
                            "No subfolders here",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> LazyColumn {
                            items(entries, key = { it.absolutePath }) { dir ->
                                ListItem(
                                    headlineContent = { Text(dir.name) },
                                    leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                                    modifier = Modifier.clickable { currentDir = dir }
                                )
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onFolderSelected(currentDir.absolutePath) }) { Text("Select this folder") }
                }
            }
        }
    }
}

private fun listStorageRoots(context: Context): List<StorageRoot> {
    val roots = mutableListOf(StorageRoot("Internal Storage", Environment.getExternalStorageDirectory()))
    // StorageVolume.directory (the public, non-reflection way to get a real path for
    // secondary volumes like an SD card) only exists from API 30 onward.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val storageManager = context.getSystemService(StorageManager::class.java)
            storageManager?.storageVolumes?.forEach { volume ->
                if (!volume.isPrimary) {
                    volume.directory?.let { dir ->
                        roots.add(StorageRoot(volume.getDescription(context) ?: dir.name, dir))
                    }
                }
            }
        } catch (e: Exception) {
            // Fall back to internal storage only.
        }
    }
    return roots
}
