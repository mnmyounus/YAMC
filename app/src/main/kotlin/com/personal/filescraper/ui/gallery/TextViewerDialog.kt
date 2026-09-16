package com.personal.filescraper.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_PREVIEW_BYTES = 300_000

@Composable
fun TextViewerDialog(path: String, onDismiss: () -> Unit) {
    var content by remember { mutableStateOf<String?>(null) }
    var truncated by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        val result = withContext(Dispatchers.IO) {
            try {
                val bytes = File(path).readBytes()
                if (bytes.size > MAX_PREVIEW_BYTES) {
                    String(bytes, 0, MAX_PREVIEW_BYTES, Charsets.UTF_8) to true
                } else {
                    String(bytes, Charsets.UTF_8) to false
                }
            } catch (e: Exception) {
                "Couldn't open this file." to false
            }
        }
        content = result.first
        truncated = result.second
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            if (content == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(
                        text = content.orEmpty() + if (truncated) "\n\n… (truncated - file is large)" else "",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}
