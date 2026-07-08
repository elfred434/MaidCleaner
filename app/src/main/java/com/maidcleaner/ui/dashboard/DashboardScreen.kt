package com.maidcleaner.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maidcleaner.ui.common.*
import com.maidcleaner.util.SizeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCorpseFinder: () -> Unit,
    onNavigateToSystemCleaner: () -> Unit,
    onNavigateToAppCleaner: () -> Unit,
    onNavigateToStorageAnalyzer: () -> Unit,
    onNavigateToDuplicateFinder: () -> Unit,
    onNavigateToAppControl: () -> Unit,
    onNavigateToScheduler: () -> Unit,
    onNavigateToOptimizer: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MaidCleaner",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Storage Health Ring
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Storage Health",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    StorageRingChart(usedPercentage = state.storageStats.usedPercentage)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Storage details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = SizeFormatter.format(state.storageStats.usedBytes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Used",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = SizeFormatter.format(state.storageStats.freeBytes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Free",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = SizeFormatter.format(state.storageStats.totalBytes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Scan Button
            if (!state.isQuickScanning) {
                Button(
                    onClick = { viewModel.quickScan() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Scan", fontWeight = FontWeight.SemiBold)
                }
            } else {
                ScanProgressIndicator(
                    progress = state.quickScanProgress,
                    message = state.quickScanMessage
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scan Summary Cards
            if (state.scanSummary.junkFileCount > 0 || state.scanSummary.corpseFileCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.scanSummary.junkFileCount > 0) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    SizeFormatter.format(state.scanSummary.junkSize),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "${state.scanSummary.junkFileCount} junk files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    if (state.scanSummary.corpseFileCount > 0) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FolderOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    SizeFormatter.format(state.scanSummary.corpseSize),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "${state.scanSummary.corpseFileCount} orphaned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Root/Shizuku Status
            val rootStatus = state.rootStatus
            if (rootStatus.hasFullAccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                rootStatus.isRooted -> "Root Access Available"
                                rootStatus.isShizukuAvailable -> "Shizuku Available"
                                else -> "Enhanced Access"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Module Cards
            Text(
                text = "Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.FolderOff,
                title = "Corpse Finder",
                subtitle = "Find orphaned files from uninstalled apps",
                onClick = onNavigateToCorpseFinder
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.CleaningServices,
                title = "System Cleaner",
                subtitle = "Clear cache, temp files, and junk",
                badge = if (state.scanSummary.junkFileCount > 0)
                    SizeFormatter.format(state.scanSummary.junkSize) else null,
                onClick = onNavigateToSystemCleaner
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.Apps,
                title = "App Cleaner",
                subtitle = "Per-app cache and data breakdown",
                onClick = onNavigateToAppCleaner
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.SettingsApplications,
                title = "App Control",
                subtitle = "Manage installed applications",
                onClick = onNavigateToAppControl
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.PieChart,
                title = "Storage Analyzer",
                subtitle = "Visual breakdown of storage usage",
                onClick = onNavigateToStorageAnalyzer
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.ContentCopy,
                title = "Duplicate Finder",
                subtitle = "Find duplicate files by content",
                onClick = onNavigateToDuplicateFinder
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.Speed,
                title = "Optimizer",
                subtitle = if (rootStatus.hasFullAccess) "Optimize SQLite databases"
                else "Optimize databases (requires root/Shizuku)",
                onClick = onNavigateToOptimizer
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModuleCard(
                icon = Icons.Default.Schedule,
                title = "Scheduler",
                subtitle = "Schedule recurring scans",
                onClick = onNavigateToScheduler
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Data Safety Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Data Safety",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "MaidCleaner collects NO personal data. All scanning and cleaning is performed locally on your device. No data is transmitted to any server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
