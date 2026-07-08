package com.maidcleaner.ui.appcontrol

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maidcleaner.ui.common.MaidTopAppBar
import com.maidcleaner.util.DateFormatter
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppControlScreen(
    onBack: () -> Unit,
    viewModel: AppControlViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var showActionsForApp by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            MaidTopAppBar(
                title = "App Control",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.selectAll() }) {
                        Icon(Icons.Default.SelectAll, "Select All")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        AppSortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name.replace("_", " ")) },
                                onClick = {
                                    viewModel.setSortMode(mode)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (mode == state.sortMode) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.exportAppList(context) }) {
                        Icon(Icons.Default.Upload, "Export")
                    }
                }
            )
        },
        bottomBar = {
            if (state.selectedApps.isNotEmpty()) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.selectedApps.size} selected",
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.deselectAll() }) {
                                Text("Deselect")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.uninstallSelected(context) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text("Uninstall")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilterChip(
                    selected = state.showSystemApps,
                    onClick = { viewModel.toggleShowSystemApps() },
                    label = { Text("System") }
                )
            }

            // Export success message
            state.exportedPath?.let { path ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exported to: $path", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // App list
            if (state.isScanning) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading apps...")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.filteredApps, key = { it.packageName }) { app ->
                        AppControlCard(
                            app = app,
                            isSelected = app.packageName in state.selectedApps,
                            onSelect = { viewModel.toggleAppSelection(app.packageName) },
                            onShowActions = { showActionsForApp = app.packageName }
                        )
                    }
                }
            }
        }

        // Action sheet for individual app
        showActionsForApp?.let { pkgName ->
            val app = state.filteredApps.find { it.packageName == pkgName }
            if (app != null) {
                AppActionSheet(
                    app = app,
                    onDismiss = { showActionsForApp = null },
                    onUninstall = {
                        viewModel.uninstallApp(context, pkgName)
                        showActionsForApp = null
                    },
                    onForceStop = {
                        viewModel.forceStopApp(context, pkgName)
                        showActionsForApp = null
                    },
                    onClearData = {
                        viewModel.clearAppData(context, pkgName)
                        showActionsForApp = null
                    }
                )
            }
        }
    }
}

@Composable
fun AppControlCard(
    app: com.maidcleaner.data.model.AppInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onShowActions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                if (app.isSystemApp) Icons.Default.Settings else Icons.Default.Apps,
                null,
                modifier = Modifier.size(36.dp),
                tint = if (app.isSystemApp) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${SizeFormatter.format(app.totalSize)} • ${DateFormatter.formatRelative(app.installDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (app.isSystemApp) {
                    Text(
                        "System App",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            IconButton(onClick = onShowActions) {
                Icon(Icons.Default.MoreVert, "Actions")
            }
        }
    }
}

@Composable
fun AppActionSheet(
    app: com.maidcleaner.data.model.AppInfo,
    onDismiss: () -> Unit,
    onUninstall: () -> Unit,
    onForceStop: () -> Unit,
    onClearData: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.appName) },
        text = {
            Column {
                Text("Package: ${app.packageName}", style = MaterialTheme.typography.bodySmall)
                Text("Version: ${app.versionName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Size: ${SizeFormatter.format(app.totalSize)}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {},
        buttons = {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!app.isSystemApp) {
                    TextButton(onClick = onUninstall) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uninstall")
                    }
                }
                TextButton(onClick = onForceStop) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Force Stop")
                }
                TextButton(onClick = onClearData) {
                    Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Data / Cache")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
