package com.maidcleaner.ui.scheduler

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
import com.maidcleaner.data.model.ScanFrequency
import com.maidcleaner.data.model.ScanModule
import com.maidcleaner.data.model.ScheduledScan
import com.maidcleaner.ui.common.MaidTopAppBar
import com.maidcleaner.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    onBack: () -> Unit,
    viewModel: SchedulerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            MaidTopAppBar(title = "Scheduler", onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateSheet() }) {
                Icon(Icons.Default.Add, "New Schedule")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.schedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Scheduled Scans", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create a schedule to automatically scan your device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onToggle = { viewModel.toggleScheduleEnabled(schedule) },
                            onDelete = { viewModel.deleteSchedule(schedule) }
                        )
                    }
                }
            }
        }

        // Create schedule dialog
        if (state.isCreating) {
            CreateScheduleDialog(
                state = state,
                onSetFrequency = { viewModel.setFrequency(it) },
                onSetHour = { viewModel.setHour(it) },
                onSetMinute = { viewModel.setMinute(it) },
                onToggleModule = { viewModel.toggleModule(it) },
                onCreate = { viewModel.createSchedule() },
                onDismiss = { viewModel.hideCreateSheet() }
            )
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: ScheduledScan,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    schedule.frequency.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "At ${String.format("%02d:%02d", schedule.hourOfDay, schedule.minuteOfHour)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Modules: ${schedule.modules.joinToString { it.name.replace("_", " ") }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (schedule.lastRunTime > 0) {
                    Text(
                        "Last run: ${DateFormatter.formatRelative(schedule.lastRunTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = { onToggle() }
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CreateScheduleDialog(
    state: SchedulerState,
    onSetFrequency: (ScanFrequency) -> Unit,
    onSetHour: (Int) -> Unit,
    onSetMinute: (Int) -> Unit,
    onToggleModule: (ScanModule) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(state.newScheduleHour) }
    var minute by remember { mutableStateOf(state.newScheduleMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Scheduled Scan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Frequency
                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = state.newScheduleFrequency == freq,
                            onClick = { onSetFrequency(freq) },
                            label = { Text(freq.name) }
                        )
                    }
                }

                // Time
                Text("Time", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = String.format("%02d", hour),
                        onValueChange = {
                            it.toIntOrNull()?.let { h ->
                                if (h in 0..23) {
                                    hour = h
                                    onSetHour(h)
                                }
                            }
                        },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":")
                    OutlinedTextField(
                        value = String.format("%02d", minute),
                        onValueChange = {
                            it.toIntOrNull()?.let { m ->
                                if (m in 0..59) {
                                    minute = m
                                    onSetMinute(m)
                                }
                            }
                        },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Modules
                Text("Modules to Run", style = MaterialTheme.typography.labelLarge)
                ScanModule.entries.forEach { module ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = module in state.newScheduleModules,
                            onCheckedChange = { onToggleModule(module) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(module.name.replace("_", " "))
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onCreate,
                enabled = state.newScheduleModules.isNotEmpty()
            ) {
                Text("Create Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
