package com.personal.filescraper.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.personal.filescraper.ui.components.PermissionCard
import com.personal.filescraper.util.PermissionUtils
import com.personal.filescraper.worker.SyncWorker

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val monitoring by viewModel.monitoringEnabled.collectAsStateWithLifecycle()
    val folderCount by viewModel.watchedFolderCount.collectAsStateWithLifecycle()

    var hasAllFilesAccess by remember { mutableStateOf(PermissionUtils.hasAllFilesAccess()) }
    var hasNotificationPermission by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }

    val allFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasAllFilesAccess = PermissionUtils.hasAllFilesAccess()
    }
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        hasAllFilesAccess = PermissionUtils.hasAllFilesAccess()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("Personal File Scraper", style = MaterialTheme.typography.headlineSmall)
        Text(
            "$folderCount folder(s) watched",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (!hasAllFilesAccess) {
            PermissionCard(
                title = "All files access needed",
                description = "Required to watch folders like DCIM and Android/media and copy new files into the archive.",
                buttonLabel = "Grant access",
                onClick = { allFilesLauncher.launch(PermissionUtils.allFilesAccessIntent(context)) }
            )
            Spacer(Modifier.height(12.dp))
        }
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                title = "Notifications disabled",
                description = "Enable notifications to see when files are archived and cleaned up.",
                buttonLabel = "Enable",
                onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
            Spacer(Modifier.height(12.dp))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Monitoring", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (monitoring) "Active" else "Stopped",
                        color = if (monitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = monitoring,
                    enabled = hasAllFilesAccess,
                    onCheckedChange = { enabled -> viewModel.setMonitoring(enabled, context) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build()) },
            enabled = hasAllFilesAccess,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan watched folders now")
        }
    }
}
