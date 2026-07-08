package com.maidcleaner.ui.duplicatefinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.maidcleaner.data.model.DuplicateGroup
import com.maidcleaner.ui.common.*
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFinderScreen(
    onBack: () -> Unit,
    viewModel: DuplicateFinderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            MaidTopAppBar(title = "Duplicate Finder", onBack = onBack)
        },
        bottomBar = {
            AnimatedVisibility(visible = state.selectedForDeletion.isNotEmpty()) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.selectedForDeletion.size} files to delete • ${SizeFormatter.format(state.totalSpaceReclaimable)}",
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
                            Text("Delete")
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
            // Initial state
            if (!state.isScanning && state.duplicateGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Duplicate Finder", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Find duplicate files by content hash",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.scan() }) {
                            Icon(Icons.Default.Search, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan for Duplicates")
                        }
                    }
                }
            }

            // Scanning
            if (state.isScanning) {
                ScanProgressIndicator(
                    progress = state.scanProgress,
                    message = state.scanMessage,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Results
            if (!state.isScanning && state.duplicateGroups.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary
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
                                    "${state.duplicateGroups.size} duplicate groups",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Wasted: ${SizeFormatter.format(state.duplicateGroups.sumOf { it.spaceWasted })}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Duplicate groups
                    itemsIndexed(state.duplicateGroups, key = { index, group -> group.hash }) { index, group ->
                        DuplicateGroupCard(
                            group = group,
                            groupIndex = index,
                            isExpanded = index in state.expandedGroups,
                            keptFiles = state.keptFiles,
                            selectedForDeletion = state.selectedForDeletion,
                            onToggleExpand = { viewModel.toggleGroupExpanded(index) },
                            onKeep = { viewModel.markAsKept(it) },
                            onSelectForDeletion = { viewModel.toggleForDeletion(it) }
                        )
                    }
                }
            }
        }

        // Confirm dialog
        if (state.showConfirmDialog) {
            ConfirmationDialog(
                title = "Delete Duplicates",
                message = "${state.selectedForDeletion.size} duplicate files will be permanently deleted, " +
                        "freeing ${SizeFormatter.format(state.totalSpaceReclaimable)}. Keep at least one " +
                        "copy of each file to avoid data loss.",
                confirmLabel = "Delete",
                isDestructive = true,
                onConfirm = { viewModel.deleteSelected() },
                onDismiss = { viewModel.hideConfirmDialog() }
            )
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    groupIndex: Int,
    isExpanded: Boolean,
    keptFiles: Set<String>,
    selectedForDeletion: Set<String>,
    onToggleExpand: () -> Unit,
    onKeep: (String) -> Unit,
    onSelectForDeletion: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Group header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Group ${groupIndex + 1}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${group.files.size} copies • ${SizeFormatter.format(group.fileSize)} each",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    SizeFormatter.format(group.spaceWasted),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null
                )
            }

            // Expanded: file list
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    group.files.forEach { file ->
                        val isKept = file.path in keptFiles
                        val isSelected = file.path in selectedForDeletion

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isKept) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onSelectForDeletion(file.path) }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    file.path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isKept) {
                                Text(
                                    "Keeping",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                TextButton(onClick = { onKeep(file.path) }) {
                                    Text("Keep")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
