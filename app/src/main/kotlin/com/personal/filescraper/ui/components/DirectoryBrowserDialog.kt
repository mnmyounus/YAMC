@file:Suppress("DEPRECATION")

package com.personal.filescraper.ui.components

import android.os.Environment
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun DirectoryBrowserDialog(
    onDismiss: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val entries = remember(currentDir) {
        currentDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Column(Modifier.padding(16.dp)) {
                Text(currentDir.absolutePath, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                currentDir.parentFile?.let { parent ->
                    TextButton(onClick = { currentDir = parent }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Up")
                    }
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(entries) { dir ->
                        ListItem(
                            headlineContent = { Text(dir.name) },
                            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                            modifier = Modifier.clickable { currentDir = dir }
                        )
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
