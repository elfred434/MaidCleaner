package com.maidcleaner.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Data Safety Summary composable.
 * This mirrors what will be declared on the Play Store listing.
 */
@Composable
fun DataSafetySummary(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Data Safety Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Data collected
            SectionHeader("Data Collected")
            SafetyItem(
                icon = Icons.Default.CheckCircle,
                text = "NO personal data collected",
                isPositive = true
            )
            SafetyItem(
                icon = Icons.Default.CheckCircle,
                text = "NO device identifiers collected",
                isPositive = true
            )
            SafetyItem(
                icon = Icons.Default.CheckCircle,
                text = "NO location data collected",
                isPositive = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Data shared
            SectionHeader("Data Shared")
            SafetyItem(
                icon = Icons.Default.CheckCircle,
                text = "NO data shared with third parties",
                isPositive = true
            )
            SafetyItem(
                icon = Icons.Default.CheckCircle,
                text = "NO analytics or tracking SDKs",
                isPositive = true
            )
            SafetyItem(
                icon = Icons.Default.CheckCircle,
                text = "NO ads or advertising SDKs",
                isPositive = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Data handling
            SectionHeader("Data Handling")
            SafetyItem(
                icon = Icons.Default.CloudOff,
                text = "All processing is on-device only",
                isPositive = true
            )
            SafetyItem(
                icon = Icons.Default.Lock,
                text = "Local data stored in app-private storage",
                isPositive = true
            )
            SafetyItem(
                icon = Icons.Default.DeleteForever,
                text = "Uninstalling removes all app data",
                isPositive = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Permissions
            SectionHeader("Permissions Used")
            PermissionItem("Storage", "Scan and manage files (only when you initiate a scan)")
            PermissionItem("Notifications", "Alert you when scheduled scans complete")
            PermissionItem("Query All Packages", "Identify installed apps for corpse detection")
            PermissionItem("Alarm & Wake", "Run scheduled background scans")

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Last updated: 2024",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun SafetyItem(icon: ImageVector, text: String, isPositive: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isPositive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun PermissionItem(name: String, rationale: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            Icons.Default.VerifiedUser,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(rationale, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
