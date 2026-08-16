package com.personal.filescraper.ui.folders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.filescraper.ui.components.DirectoryBrowserDialog

@Composable
fun FolderPickerScreen(viewModel: FolderPickerViewModel) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var showBrowser by remember { mutableStateOf(false) }

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
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(folders, key = { it.path }) { folder ->
                    ListItem(
                        headlineContent = { Text(folder.displayName) },
                        supportingContent = { Text(folder.path, maxLines = 1) },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                        trailingContent = {
                            if (!folder.isDefault) {
                                IconButton(onClick = { viewModel.removeFolder(folder.path, context) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
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
}
