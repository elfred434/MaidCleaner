package com.maidcleaner.ui.systemcleaner

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maidcleaner.data.model.JunkType
import com.maidcleaner.ui.common.*
import com.maidcleaner.util.DateFormatter
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemCleanerScreen(
    onBack: () -> Unit,
    viewModel: SystemCleanerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            MaidTopAppBar(
                title = "System Cleaner",
                onBack = onBack,
                actions = {
                    if (state.junkCategories.isNotEmpty()) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, "Select All")
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = state.selectedFiles.isNotEmpty()) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.selectedFiles.size} files • ${SizeFormatter.format(state.totalReclaimable)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        FilledTonalButton(
                            onClick = { viewModel.showConfirmDialog() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean")
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
            // Before/After comparison
            if (state.cleaningComplete) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Before", style = MaterialTheme.typography.labelSmall)
                            Text(
                                SizeFormatter.format(state.sizeBefore),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("After", style = MaterialTheme.typography.labelSmall)
                            Text(
                                SizeFormatter.format(state.sizeAfter),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Scan button / progress
            if (!state.isScanning && state.junkCategories.isEmpty() && !state.cleaningComplete) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("System Cleaner", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Find and clean cache, temp files, logs, and junk",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.scan() }) {
                            Icon(Icons.Default.Search, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan for Junk")
                        }
                    }
                }
            }

            if (state.isScanning) {
                ScanProgressIndicator(
                    progress = state.scanProgress,
                    message = state.scanMessage,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Results
            if (!state.isScanning && state.junkCategories.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${state.junkCategories.sumOf { it.files.size }} junk files found",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Total: ${SizeFormatter.format(state.junkCategories.sumOf { it.totalSize })}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    items(state.junkCategories, key = { it.type }) { category ->
                        JunkCategoryCard(
                            category = category,
                            isExpanded = category.type in state.expandedCategories,
                            selectedFiles = state.selectedFiles,
                            onToggleExpand = { viewModel.toggleCategoryExpanded(category.type) },
                            onSelectAll = { viewModel.selectAllInCategory(category.type) },
                            onToggleFile = { viewModel.toggleFileSelection(it) }
                        )
                    }
                }
            }
        }

        // Confirm Dialog
        if (state.showConfirmDialog) {
            ConfirmationDialog(
                title = "Clean Junk Files",
                message = "${state.selectedFiles.size} files will be deleted, freeing " +
                        "${SizeFormatter.format(state.totalReclaimable)}. This cannot be undone.",
                confirmLabel = "Clean",
                isDestructive = true,
                onConfirm = { viewModel.cleanSelected() },
                onDismiss = { viewModel.hideConfirmDialog() }
            )
        }
    }
}

@Composable
fun JunkCategoryCard(
    category: com.maidcleaner.data.model.JunkCategory,
    isExpanded: Boolean,
    selectedFiles: Set<String>,
    onToggleExpand: () -> Unit,
    onSelectAll: () -> Unit,
    onToggleFile: (String) -> Unit
) {
    val icon = when (category.type) {
        JunkType.CACHE -> Icons.Default.Cached
        JunkType.LOGS -> Icons.Default.Description
        JunkType.TEMP -> Icons.Default.DeviceThermostat
        JunkType.EMPTY_DIRS -> Icons.Default.FolderOpen
        JunkType.APK -> Icons.Default.Android
        JunkType.THUMBNAILS -> Icons.Default.Image
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.type.displayName, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${category.files.size} files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    SizeFormatter.format(category.totalSize),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null
                )
            }

            // Expanded files list
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onSelectAll) {
                            Text("Select All")
                        }
                    }
                    category.files.take(20).forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleFile(file.path) }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = file.path in selectedFiles,
                                onCheckedChange = { onToggleFile(file.path) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                Text(
                                    file.path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                SizeFormatter.format(file.size),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (category.files.size > 20) {
                        Text(
                            "  ... and ${category.files.size - 20} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
