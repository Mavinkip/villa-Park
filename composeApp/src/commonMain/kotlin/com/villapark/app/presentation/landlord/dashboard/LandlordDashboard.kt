package com.villapark.app.presentation.landlord.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.Priority
import com.villapark.app.data.models.RentalUnit
import com.villapark.app.data.models.UnitStatus
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordDashboard(
    viewModel: LandlordViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Villa Park - Landlord") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(LandlordEvent.RefreshData(true)) }) {
                        Text("Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Text("🏠", fontSize = 24.sp) },
                    label = { Text("Units") }
                )
                NavigationBarItem(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Text("🔧", fontSize = 24.sp) },
                    label = { Text("Maintenance") }
                )
                NavigationBarItem(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Text("📊", fontSize = 24.sp) },
                    label = { Text("Reports") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (state.selectedTab) {
                    0 -> UnitsTab(
                        units = state.units,
                        stats = state.stats,
                        onUnitClick = { /* Navigate to unit details */ }
                    )
                    1 -> MaintenanceTab(
                        issues = state.maintenanceIssues,
                        onMarkFixed = { issueId -> viewModel.markIssueAsFixed(issueId) },
                        onCallPlumber = { issueId -> viewModel.onEvent(LandlordEvent.CallPlumber(issueId)) }
                    )
                    2 -> ReportTab(stats = state.stats)
                }
            }
        }
    }
}

@Composable
fun UnitsTab(
    units: List<RentalUnit>,
    stats: DashboardStats,
    onUnitClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Total Units",
                    value = stats.totalUnits.toString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    title = "Occupied",
                    value = stats.occupiedUnits.toString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Stats Cards Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Overdue",
                    value = stats.overduePayments.toString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
                StatCard(
                    title = "Open Issues",
                    value = stats.openIssues.toString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Units Grid
        item {
            Text(
                text = "Units Overview",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Display units in rows of 2
        val chunkedUnits = units.chunked(2)
        items(chunkedUnits) { unitRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                unitRow.forEach { unit ->
                    UnitGridItem(
                        unit = unit,
                        onClick = { onUnitClick(unit.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add empty spacer if odd number of units
                if (unitRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
        }
    }
}

@Composable
fun UnitGridItem(
    unit: RentalUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (unit.status) {
        UnitStatus.OCCUPIED_PAID -> MaterialTheme.colorScheme.primaryContainer
        UnitStatus.OCCUPIED_OVERDUE -> MaterialTheme.colorScheme.errorContainer
        UnitStatus.MAINTENANCE_ISSUE -> MaterialTheme.colorScheme.tertiaryContainer
        UnitStatus.VACANT -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when (unit.status) {
        UnitStatus.OCCUPIED_PAID, UnitStatus.OCCUPIED_OVERDUE, UnitStatus.MAINTENANCE_ISSUE -> 
            MaterialTheme.colorScheme.onPrimaryContainer
        UnitStatus.VACANT -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = unit.number,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
                when (unit.status) {
                    UnitStatus.OCCUPIED_OVERDUE -> {
                        Text(
                            text = "⚠️ Overdue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    UnitStatus.MAINTENANCE_ISSUE -> {
                        Text(
                            text = "🔧 Issue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    UnitStatus.VACANT -> {
                        Text(
                            text = "Vacant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun MaintenanceTab(
    issues: List<MaintenanceIssue>,
    onMarkFixed: (String) -> Unit,
    onCallPlumber: (String) -> Unit
) {
    if (issues.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No maintenance issues",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(issues) { issue ->
                MaintenanceIssueCard(
                    issue = issue,
                    onMarkFixed = { onMarkFixed(issue.id) },
                    onCallPlumber = { onCallPlumber(issue.id) }
                )
            }
        }
    }
}

@Composable
fun MaintenanceIssueCard(
    issue: MaintenanceIssue,
    onMarkFixed: () -> Unit,
    onCallPlumber: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (issue.priority) {
                Priority.URGENT -> MaterialTheme.colorScheme.errorContainer
                Priority.ROUTINE -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unit ${issue.unitId}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = issue.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = issue.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                    Text(
                        text = "Reported by: ${issue.tenantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (issue.priority) {
                            Priority.URGENT -> MaterialTheme.colorScheme.error
                            Priority.ROUTINE -> MaterialTheme.colorScheme.secondary
                        }
                    )
                ) {
                    Text(
                        text = issue.priority.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCallPlumber,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Call Plumber")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onMarkFixed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Mark Fixed")
                }
            }
        }
    }
}

@Composable
fun ReportTab(stats: DashboardStats) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Revenue",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "KSh ${stats.monthlyRevenue}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Occupancy Rate",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${(stats.occupiedUnits * 100 / max(stats.totalUnits, 1))}%",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${stats.occupiedUnits} out of ${stats.totalUnits} units occupied",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Button(
                onClick = { /* Generate PDF report */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Monthly Report")
            }
        }
    }
}
