package com.maidcleaner.ui.corpsefinder

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maidcleaner.ui.common.*
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorpseFinderScreen(
    onBack: () -> Unit,
    viewModel: CorpseFinderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            MaidTopAppBar(
                title = "Corpse Finder",
                onBack = onBack,
                actions = {
                    if (state.corpseGroups.isNotEmpty()) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, "Select All")
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = state.selectedGroups.isNotEmpty()) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.selectedGroups.size} selected • ${SizeFormatter.format(state.totalReclaimable)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row {
                            TextButton(onClick = { viewModel.deselectAll() }) {
                                Text("Deselect")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.showConfirmDialog() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
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
            // Scan Button
            if (!state.isScanning && state.corpseGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOff,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Find Orphaned Files",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scan for leftover files from uninstalled apps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.scan() }) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Scan")
                        }
                    }
                }
            }

            // Scanning Progress
            if (state.isScanning) {
                ScanProgressIndicator(
                    progress = state.scanProgress,
                    message = state.scanMessage,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Results List
            if (!state.isScanning && state.corpseGroups.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary header
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
                                    "${state.corpseGroups.size} orphaned groups found",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Total: ${SizeFormatter.format(state.corpseGroups.sumOf { it.totalSize })}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    items(state.corpseGroups, key = { it.packageName }) { group ->
                        CorpseGroupCard(
                            group = group,
                            isSelected = group.packageName in state.selectedGroups,
                            onSelect = { viewModel.toggleGroupSelection(group.packageName) },
                            onWhitelist = { viewModel.addToWhitelist(group.packageName) }
                        )
                    }
                }
            }
        }

        // Confirm Deletion Dialog
        if (state.showConfirmDialog) {
            ConfirmationDialog(
                title = "Delete Orphaned Files",
                message = "${state.selectedGroups.size} group(s) will be permanently deleted. " +
                        "This frees ${SizeFormatter.format(state.totalReclaimable)}. " +
                        "This action cannot be undone.",
                confirmLabel = "Delete",
                isDestructive = true,
                onConfirm = { viewModel.deleteSelected() },
                onDismiss = { viewModel.hideConfirmDialog() }
            )
        }
    }
}

@Composable
fun CorpseGroupCard(
    group: com.maidcleaner.data.model.CorpseGroup,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onWhitelist: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.packageName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.fileCount} items • ${group.files.map { it.location.displayName }.distinct().joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = SizeFormatter.format(group.totalSize),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                IconButton(
                    onClick = onWhitelist,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.BookmarkAdd,
                        contentDescription = "Add to Whitelist",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
