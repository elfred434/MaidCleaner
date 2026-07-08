package com.maidcleaner.ui.appcleaner

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
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCleanerScreen(
    onBack: () -> Unit,
    viewModel: AppCleanerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            MaidTopAppBar(
                title = "App Cleaner",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary card
            if (state.apps.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total Cache",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                SizeFormatter.format(state.totalCacheSize),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.clearAllCaches(context) }
                        ) {
                            Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All Caches")
                        }
                    }
                }
            }

            // Search & Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                    selected = state.showExcessiveOnly,
                    onClick = { viewModel.toggleShowExcessiveOnly() },
                    label = { Text("Excessive") },
                    leadingIcon = {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scanning progress
            if (state.isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading apps...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // App list
            if (!state.isScanning) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.filteredApps, key = { it.packageName }) { app ->
                        AppCleanerCard(
                            app = app,
                            onClearCache = { viewModel.clearAppCache(context, app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppCleanerCard(
    app: com.maidcleaner.data.model.AppInfo,
    onClearCache: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (app.hasExcessiveCache)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
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
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("App: ${SizeFormatter.format(app.appSize)}", style = MaterialTheme.typography.labelSmall)
                    Text("Data: ${SizeFormatter.format(app.dataSize)}", style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    SizeFormatter.format(app.cacheSize),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (app.hasExcessiveCache) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text("cache", style = MaterialTheme.typography.labelSmall)
                if (app.hasExcessiveCache) {
                    Text(
                        "Excessive!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledTonalButton(
                    onClick = onClearCache,
                    modifier = Modifier.padding(top = 4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
