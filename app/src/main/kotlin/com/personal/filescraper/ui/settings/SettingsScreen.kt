package com.personal.filescraper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.filescraper.data.repository.SettingsRepository

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val retentionHours by viewModel.retentionHours.collectAsStateWithLifecycle()
    var textValue by remember(retentionHours) { mutableStateOf(retentionHours.toString()) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Auto-delete after", style = MaterialTheme.typography.titleMedium)
        Text(
            "Archived files older than this are permanently deleted, along with their database entry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it.filter(Char::isDigit) },
            label = { Text("Hours") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row {
            listOf(24 to "24h", 48 to "48h", 72 to "72h", 168 to "7d").forEach { (hours, label) ->
                AssistChip(
                    onClick = { textValue = hours.toString() },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val hours = textValue.toIntOrNull()?.coerceIn(1, 8760) ?: SettingsRepository.DEFAULT_RETENTION_HOURS
                viewModel.setRetentionHours(hours)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}
