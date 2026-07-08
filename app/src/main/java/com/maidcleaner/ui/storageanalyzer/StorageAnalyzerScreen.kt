package com.maidcleaner.ui.storageanalyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maidcleaner.data.model.FileType
import com.maidcleaner.data.model.StorageCategory
import com.maidcleaner.ui.common.MaidTopAppBar
import com.maidcleaner.ui.common.ScanProgressIndicator
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(
    onBack: () -> Unit,
    viewModel: StorageAnalyzerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val handleBack: () -> Unit = if (state.drillDownStack.isNotEmpty()) {
        { viewModel.navigateUp() }
    } else {
        onBack
    }

    Scaffold(
        topBar = {
            MaidTopAppBar(
                title = if (state.drillDownStack.isNotEmpty()) {
                    state.drillDownStack.last().name
                } else "Storage Analyzer",
                onBack = handleBack
            )
        }
    ) { paddingValues ->
        when {
            state.isScanning -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ScanProgressIndicator(
                        progress = state.scanProgress,
                        message = state.scanMessage
                    )
                }
            }
            state.categories.isNotEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Category breakdown chart
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Storage Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Horizontal stacked bar
                            StorageBreakdownBar(categories = state.categories)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Legend
                            state.categories.forEach { category ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(category.color))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${SizeFormatter.format(category.size)} (${
                                            String.format(
                                                "%.1f",
                                                category.percentage
                                            )
                                        }%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // File type filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedTypeFilter == null,
                            onClick = { viewModel.setTypeFilter(null) },
                            label = { Text("All") }
                        )
                        FileType.entries.forEach { type ->
                            FilterChip(
                                selected = state.selectedTypeFilter == type,
                                onClick = { viewModel.setTypeFilter(type) },
                                label = {
                                    Text(
                                        type.name.lowercase()
                                            .replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Largest files / drill-down
                    Text(
                        if (state.drillDownStack.isNotEmpty()) "Contents"
                        else "Largest Files",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.filteredFiles, key = { it.path }) { file ->
                            FileEntryCard(
                                entry = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.drillDownInto(file)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading storage data...")
                    }
                }
            }
        }
    }
}

@Composable
fun StorageBreakdownBar(categories: List<StorageCategory>) {
    val totalSize = categories.sumOf { it.size }
    if (totalSize == 0L) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        categories.forEach { category ->
            val fraction = category.size.toFloat() / totalSize.toFloat()
            if (fraction > 0.005f) {
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(Color(category.color))
                )
            }
        }
    }
}

@Composable
fun FileEntryCard(
    entry: com.maidcleaner.data.model.FileEntry,
    onClick: () -> Unit
) {
    val icon = when (entry.fileType) {
        FileType.VIDEO -> Icons.Default.VideoFile
        FileType.AUDIO -> Icons.Default.AudioFile
        FileType.IMAGE -> Icons.Default.Image
        FileType.DOCUMENT -> Icons.Default.Description
        FileType.ARCHIVE -> Icons.Default.FolderZip
        FileType.APK -> Icons.Default.Android
        FileType.OTHER -> if (entry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.isDirectory, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entry.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                SizeFormatter.format(entry.size),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (entry.isDirectory) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
