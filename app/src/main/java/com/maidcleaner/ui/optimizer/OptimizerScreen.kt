package com.maidcleaner.ui.optimizer

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
import com.maidcleaner.ui.common.MaidTopAppBar
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizerScreen(
    onBack: () -> Unit,
    viewModel: OptimizerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            MaidTopAppBar(title = "Database Optimizer", onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Root/Shizuku status banner
            if (!state.rootStatus.hasFullAccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Database optimization requires root or Shizuku access. " +
                                    "Only public databases can be optimized without elevated permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (state.totalSavings > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Total savings: ${SizeFormatter.format(state.totalSavings)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Scan / Optimize buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.scan() },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isScanning
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Databases")
                }
                if (state.databases.any { it.isOptimizable }) {
                    FilledTonalButton(
                        onClick = { viewModel.optimizeAll() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isOptimizing
                    ) {
                        Icon(Icons.Default.Speed, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimize All")
                    }
                }
            }

            if (state.isScanning) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Scanning databases...")
                    }
                }
            }

            // Database list
            if (state.databases.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.databases, key = { it.path }) { db ->
                        DatabaseCard(
                            dbInfo = db,
                            isOptimizing = state.isOptimizing && state.optimizingDbName == db.name,
                            optimizationResult = state.optimizationResults[db.name],
                            onOptimize = { viewModel.optimizeDatabase(db) }
                        )
                    }
                }
            }

            // Empty state
            if (!state.isScanning && state.databases.isEmpty() && state.totalSavings == 0L) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Speed,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No databases scanned yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap Scan to find optimizable databases",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DatabaseCard(
    dbInfo: com.maidcleaner.data.model.DatabaseInfo,
    isOptimizing: Boolean,
    optimizationResult: Pair<Long, Long>?,
    onOptimize: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dbInfo.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    dbInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (optimizationResult != null) {
                    val (before, after) = optimizationResult
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Before: ${SizeFormatter.format(before)}", style = MaterialTheme.typography.labelSmall)
                        Text("→ After: ${SizeFormatter.format(after)}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Saved: ${SizeFormatter.format(before - after)}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    SizeFormatter.format(dbInfo.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (dbInfo.isOptimizable) {
                    Text(
                        "Est. savings: ${SizeFormatter.format(dbInfo.estimatedSavings)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (isOptimizing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(top = 4.dp))
                } else if (dbInfo.isOptimizable && optimizationResult == null) {
                    FilledTonalButton(
                        onClick = onOptimize,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("VACUUM", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
