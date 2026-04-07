package com.villapark.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.SlideTransition
import com.villapark.app.data.models.PaymentType
import com.villapark.app.presentation.auth.LoginScreenContent
import com.villapark.app.presentation.landlord.dashboard.LandlordViewModel
import com.villapark.app.presentation.tenant.home.TenantHomeViewModel
import com.villapark.app.presentation.tenant.payments.PaymentViewModel

@Composable
fun AppNavigation() {
    Navigator(screen = LoginScreen) { navigator ->
        SlideTransition(navigator)
    }
}

data object LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        LoginScreenContent(
            onTenantLogin = { tenantId -> navigator.replace(TenantHomeScreen(tenantId)) },
            onLandlordLogin = { navigator.replace(LandlordDashboardScreen) }
        )
    }
}

data class TenantHomeScreen(val tenantId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<TenantHomeViewModel>()
        com.villapark.app.presentation.tenant.home.TenantHomeScreen(
            viewModel = viewModel,
            tenantId = tenantId,
            onNavigateToPayment = { type -> navigator.push(PaymentScreen(tenantId, type)) },
            onNavigateToIssueReport = { navigator.push(IssueReportScreen(tenantId)) },
            onNavigateToPaymentHistory = { navigator.push(PaymentHistoryScreen(tenantId)) }
        )
    }
}

data class PaymentScreen(
    val tenantId: String,
    val paymentType: PaymentType = PaymentType.RENT
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<PaymentViewModel>()
        com.villapark.app.presentation.tenant.payments.PaymentScreenContent(
            viewModel = viewModel,
            tenantId = tenantId,
            paymentType = paymentType,
            onBack = { navigator.pop() }
        )
    }
}

data class IssueReportScreen(val tenantId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        PlaceholderScreen("Report Issue", "Coming in Session 6") { navigator.pop() }
    }
}

data class PaymentHistoryScreen(val tenantId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        PlaceholderScreen("Payment History", "Coming in Session 7") { navigator.pop() }
    }
}

data object LandlordDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<LandlordViewModel>()
        com.villapark.app.presentation.landlord.dashboard.LandlordDashboard(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, subtitle: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚧", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
