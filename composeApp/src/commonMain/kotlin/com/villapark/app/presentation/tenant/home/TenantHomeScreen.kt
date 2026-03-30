package com.villapark.app.presentation.tenant.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.Priority
import com.villapark.app.data.models.Utilities
import com.villapark.app.data.models.UtilityStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantHomeScreen(
    viewModel: TenantHomeViewModel,
    tenantId: String,
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
            ) {
                Text("📷", fontSize = 24.sp)  // Camera emoji
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.tenant == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Welcome Header
                    item {
                        Text(
                            text = "👋 Welcome back, ${state.tenant?.name ?: "Tenant"}!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    // Current Balance Card
                    item {
                        CurrentBalanceCard(
                            balance = state.tenant?.currentBalance ?: 0.0,
                            unitNumber = state.unit?.number ?: "--"
                        )
                    }

                    // Quick Actions
                    item {
                        QuickActionsRow(
                            onPayRent = {
                                state.tenant?.let { tenant ->
                                    viewModel.onEvent(
                                        TenantHomeEvent.PayRent(
                                            tenantId = tenant.id,
                                            amount = tenant.currentBalance,
                                            phoneNumber = tenant.phoneNumber
                                        )
                                    )
                                }
                            },
                            onViewHistory = onNavigateToPaymentHistory
                        )
                    }

                    // Utilities Section - Check if unit has utilities
                    if (state.unit?.utilities != null) {
                        item {
                            UtilitiesSection(utilities = state.unit!!.utilities)
                        }
                    }

                    // Active Maintenance Issues
                    if (state.maintenanceIssues.isNotEmpty()) {
                        item {
                            ActiveIssuesSection(
                                issues = state.maintenanceIssues,
                                onViewAll = { /* Navigate to all issues */ }
                            )
                        }
                    }
                }
            }

            // Error Snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = {
                            viewModel.onEvent(TenantHomeEvent.RefreshData(tenantId))
                        }) {
                            Text("🔄 Retry")
                        }
                    }
                ) {
                    Text("⚠️ $error")
                }
            }
        }
    }

    // Payment Progress Dialog
    if (state.showPaymentDialog) {
        PaymentProgressDialog(
            paymentState = state.paymentState,
            onDismiss = { viewModel.onEvent(TenantHomeEvent.DismissPaymentDialog) }
        )
    }
}

@Composable
fun CurrentBalanceCard(
    balance: Double,
    unitNumber: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "💰 Current Balance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "KSh ${formatAmount(balance)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "🏠 Unit $unitNumber",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (balance <= 0) "✅ Paid" else "⚠️ Due",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (balance <= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Helper function to format currency (works on all platforms)
private fun formatAmount(amount: Double): String {
    val rounded = (amount * 100).toLong() / 100.0
    val parts = rounded.toString().split(".")
    val integerPart = parts[0]
    val decimalPart = parts.getOrElse(1) { "00" }.padEnd(2, '0').take(2)

    // Add thousand separators
    val formattedInteger = integerPart.reversed().chunked(3).joinToString(",").reversed()

    return "$formattedInteger.$decimalPart"
}

@Composable
fun QuickActionsRow(
    onPayRent: () -> Unit,
    onViewHistory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onPayRent,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("💰", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pay Rent")
        }

        OutlinedButton(
            onClick = onViewHistory,
            modifier = Modifier.weight(1f)
        ) {
            Text("📜", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("History")
        }
    }
}

@Composable
fun UtilitiesSection(
    utilities: Utilities
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💡 Utilities",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            val utilityItems = listOf(
                UtilityItem("Water", utilities.water, "💧"),
                UtilityItem("Electricity", utilities.electricity, "⚡"),
                UtilityItem("Trash", utilities.trash, "🗑️")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(utilityItems) { item ->
                    UtilityChip(
                        name = item.name,
                        status = item.status,
                        emoji = item.emoji
                    )
                }
            }
        }
    }
}

// Data class for utility items
private data class UtilityItem(
    val name: String,
    val status: UtilityStatus,
    val emoji: String
)

@Composable
fun UtilityChip(
    name: String,
    status: UtilityStatus,
    emoji: String
) {
    AssistChip(
        onClick = { /* View details */ },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(name)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (status == UtilityStatus.PAID)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        trailingIcon = {
            Text(
                text = if (status == UtilityStatus.PAID) "✅" else "⚠️",
                fontSize = 12.sp
            )
        }
    )
}

@Composable
fun ActiveIssuesSection(
    issues: List<MaintenanceIssue>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔧 Active Issues",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onViewAll) {
                    Text("View All →")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            issues.take(3).forEachIndexed { index, issue ->
                MaintenanceIssueItem(issue = issue)
                if (index < issues.take(3).size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun MaintenanceIssueItem(
    issue: MaintenanceIssue
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (issue.priority == Priority.URGENT) "🚨 " else "📋 ",
                    fontSize = 16.sp
                )
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = "📅 Reported on ${issue.createdAt?.toString() ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (issue.priority) {
                    Priority.URGENT -> MaterialTheme.colorScheme.errorContainer
                    Priority.ROUTINE -> MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Text(
                text = if (issue.priority == Priority.URGENT) "🔴 URGENT" else "🟡 Routine",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun PaymentProgressDialog(
    paymentState: PaymentState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (paymentState) {
                    is PaymentState.Processing -> "⏳ Processing Payment"
                    is PaymentState.Success -> "✅ Payment Successful!"
                    is PaymentState.Failed -> "❌ Payment Failed"
                    PaymentState.Idle -> ""
                }
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (paymentState) {
                    is PaymentState.Processing -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("📱 Check your phone for the M-Pesa prompt...")
                    }
                    is PaymentState.Success -> {
                        Text("✅", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("💰 Payment of KSH ${formatAmount(paymentState.amount)} completed!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📄 Reference: ${paymentState.reference}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is PaymentState.Failed -> {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Payment failed: ${paymentState.error}")
                    }
                    PaymentState.Idle -> {}
                }
            }
        },
        confirmButton = {
            when (paymentState) {
                is PaymentState.Success -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { /* Download receipt */ }) {
                            Text("📥 Download Receipt")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
                is PaymentState.Failed -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(onClick = { /* Retry */ }) {
                            Text("🔄 Try Again")
                        }
                    }
                }
                is PaymentState.Processing -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
                PaymentState.Idle -> {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    )
}