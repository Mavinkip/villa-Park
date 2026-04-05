package com.villapark.app.presentation.tenant.home

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/presentation/tenant/home/TenantHomeScreen.kt
// ACTION: REPLACE entire file

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.PaymentType
import com.villapark.app.data.models.Priority
import com.villapark.app.data.models.UtilityStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantHomeScreen(
    viewModel: TenantHomeViewModel,
    tenantId: String,
    onNavigateToPayment: (PaymentType) -> Unit,
    onNavigateToIssueReport: () -> Unit,
    onNavigateToPaymentHistory: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(TenantHomeEvent.LoadData(tenantId))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToIssueReport,
                containerColor = MaterialTheme.colorScheme.primary
            ) { Text("📷", fontSize = 24.sp) }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading && state.tenant == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "👋 Welcome, ${state.tenant?.name ?: "Tenant"}!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    item {
                        BalanceCard(
                            balance = state.tenant?.currentBalance ?: 0.0,
                            unitNumber = state.unit?.number ?: "--"
                        )
                    }
                    item {
                        PaymentActionsSection(
                            onPayRent = { onNavigateToPayment(PaymentType.RENT) },
                            onBuyTokens = { onNavigateToPayment(PaymentType.KPLC_TOKEN) },
                            onPayInternet = { onNavigateToPayment(PaymentType.INTERNET) },
                            onViewHistory = onNavigateToPaymentHistory
                        )
                    }
                    if (state.maintenanceIssues.isNotEmpty()) {
                        item {
                            ActiveIssuesCard(issues = state.maintenanceIssues)
                        }
                    }
                }
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = {
                            viewModel.onEvent(TenantHomeEvent.RefreshData(tenantId))
                        }) { Text("Retry") }
                    }
                ) { Text(error) }
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Double, unitNumber: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Current Balance", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("KSh ${formatAmount(balance)}", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Unit $unitNumber", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    text = if (balance <= 0) "✅ Paid" else "⚠️ Due",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (balance <= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PaymentActionsSection(
    onPayRent: () -> Unit,
    onBuyTokens: () -> Unit,
    onPayInternet: () -> Unit,
    onViewHistory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPayRent, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💰", fontSize = 20.sp)
                    Text("Pay Rent", style = MaterialTheme.typography.labelMedium)
                }
            }
            Button(
                onClick = onBuyTokens, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 20.sp)
                    Text("Buy Tokens", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPayInternet, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📶", fontSize = 20.sp)
                    Text("Internet", style = MaterialTheme.typography.labelMedium)
                }
            }
            OutlinedButton(onClick = onViewHistory, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📜", fontSize = 20.sp)
                    Text("History", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ActiveIssuesCard(issues: List<MaintenanceIssue>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Active Issues", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            issues.take(3).forEachIndexed { index, issue ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${if (issue.priority == Priority.URGENT) "🚨" else "📋"} ${issue.title}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text("Reported: ${issue.createdAt?.toString() ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (issue.priority == Priority.URGENT)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    )) {
                        Text(
                            if (issue.priority == Priority.URGENT) "URGENT" else "Routine",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (index < minOf(issues.size, 3) - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    val rounded = (amount * 100).toLong() / 100.0
    val parts = rounded.toString().split(".")
    val integerPart = parts[0]
    val decimalPart = parts.getOrElse(1) { "00" }.padEnd(2, '0').take(2)
    return "${integerPart.reversed().chunked(3).joinToString(",").reversed()}.$decimalPart"
}