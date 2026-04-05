package com.villapark.app.presentation.tenant.payments

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/presentation/tenant/payments/PaymentScreen.kt
// ACTION: REPLACE entire file (was empty)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.villapark.app.data.models.PaymentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreenContent(
    viewModel: PaymentViewModel,
    tenantId: String,
    paymentType: PaymentType,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(PaymentEvent.LoadConfig(tenantId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (paymentType) {
                        PaymentType.RENT -> "Pay Rent"
                        PaymentType.KPLC_TOKEN -> "Buy Electricity Tokens"
                        PaymentType.INTERNET -> "Pay Internet Bill"
                    })
                },
                navigationIcon = {
                    // Hide back button while waiting for PIN — keeps tenant on screen
                    if (state.stage is PaymentFlowStage.Form || state.stage is PaymentFlowStage.Failed) {
                        IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val stage = state.stage) {
                is PaymentFlowStage.Form ->
                    FormContent(state, paymentType, tenantId, viewModel::onEvent)
                is PaymentFlowStage.WaitingForPin ->
                    WaitingContent()
                is PaymentFlowStage.Success ->
                    SuccessContent(state, paymentType, onBack)
                is PaymentFlowStage.Failed ->
                    FailedContent(stage.message, { viewModel.onEvent(PaymentEvent.Retry) }, onBack)
            }
        }
    }
}

// ── FORM ──────────────────────────────────────────────────────────────────────

@Composable
private fun FormContent(
    state: PaymentUiState,
    paymentType: PaymentType,
    tenantId: String,
    onEvent: (PaymentEvent) -> Unit
) {
    val unitId = "unit_placeholder" // will come from tenant data in Session 5

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Destination card — shows WHERE money is going before tenant pays
        DestinationCard(state, paymentType)

        if (paymentType == PaymentType.KPLC_TOKEN) {
            OutlinedTextField(
                value = state.meterNumber,
                onValueChange = { onEvent(PaymentEvent.MeterNumberChanged(it)) },
                label = { Text("Meter Number") },
                placeholder = { Text("e.g. 12345678") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = state.meterError != null,
                supportingText = { Text(state.meterError ?: "Found on your meter or KPLC bill") }
            )
        }

        OutlinedTextField(
            value = state.amount,
            onValueChange = { onEvent(PaymentEvent.AmountChanged(it)) },
            label = { Text("Amount (KSh)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("KSh ") },
            isError = state.amountError != null,
            supportingText = state.amountError?.let { { Text(it) } }
        )

        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = { onEvent(PaymentEvent.PhoneChanged(it)) },
            label = { Text("M-Pesa Phone Number") },
            placeholder = { Text("e.g. 0712 345 678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            isError = state.phoneError != null,
            supportingText = { Text(state.phoneError ?: "You'll get an M-Pesa prompt on this number") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onEvent(PaymentEvent.Submit(tenantId, unitId, paymentType)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoadingConfig
        ) {
            if (state.isLoadingConfig) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Pay via M-Pesa")
        }

        Text(
            "You will receive an M-Pesa prompt on your phone. Enter your PIN to complete payment.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DestinationCard(state: PaymentUiState, paymentType: PaymentType) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (paymentType) {
                PaymentType.RENT -> {
                    Text("Paying rent to", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Paybill: ${state.propertyConfig.rentPaybill.ifBlank { "Loading..." }}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Account: ${state.propertyConfig.rentAccountPrefix}-[your unit]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                PaymentType.KPLC_TOKEN -> {
                    Text("Buying electricity tokens", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("KPLC Paybill: ${state.propertyConfig.kplcPaybill}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Your token will appear here after payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                PaymentType.INTERNET -> {
                    Text("Paying internet bill to", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Till: ${state.propertyConfig.internetTillNumber.ifBlank { "Loading..." }}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ── WAITING ───────────────────────────────────────────────────────────────────

@Composable
private fun WaitingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text("Check your phone", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "An M-Pesa prompt has been sent to your phone.\n\nEnter your M-Pesa PIN to complete the payment.\n\nDo not close this screen.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── SUCCESS ───────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(state: PaymentUiState, paymentType: PaymentType, onDone: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val payment = state.completedPayment

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("✅", fontSize = 64.sp)
        Text("Payment Successful!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReceiptRow("Amount", "KSh ${payment?.amount ?: ""}")
                ReceiptRow("M-Pesa Receipt", payment?.mpesaReceiptNumber?.ifBlank { "—" } ?: "—")
                ReceiptRow("Reference", payment?.payHeroReference?.ifBlank { "—" } ?: "—")
                ReceiptRow("Type", paymentType.name.replace("_", " "))
            }
        }

        // KPLC TOKEN — only shown when electricity was purchased and token exists
        if (paymentType == PaymentType.KPLC_TOKEN && payment?.kplcToken?.isNotBlank() == true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚡ Your Electricity Token", style = MaterialTheme.typography.titleLarge)
                    Text("Enter this code into your meter:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = payment.kplcToken,
                        style = MaterialTheme.typography.headlineMedium,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (payment.kplcUnits > 0) {
                        Text("${payment.kplcUnits} kWh purchased", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(payment.kplcToken))
                    }) { Text("Copy Token") }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            "⚠️ Save this token — you need it to activate electricity in your unit.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── FAILED ────────────────────────────────────────────────────────────────────

@Composable
private fun FailedContent(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("❌", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Payment Not Completed", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}