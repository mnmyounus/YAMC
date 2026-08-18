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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    // Off by default so ordinary browsing stays uncluttered - toggle on to dig
    // into dot-prefixed folders (e.g. hunting for an old .git/project folder).
    var showHidden by remember { mutableStateOf(false) }

    LaunchedEffect(currentDir, showHidden) {
        isLoading = true
        entries = withContext(Dispatchers.IO) {
            try {
                currentDir.listFiles()
                    ?.filter { it.isDirectory && (showHidden || !it.name.startsWith(".")) }
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentDir.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showHidden = !showHidden }) {
                        Icon(
                            if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showHidden) "Hide hidden folders" else "Show hidden folders"
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

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
                                val isHidden = dir.name.startsWith(".")
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            dir.name,
                                            color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                                        )
                                    },
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
