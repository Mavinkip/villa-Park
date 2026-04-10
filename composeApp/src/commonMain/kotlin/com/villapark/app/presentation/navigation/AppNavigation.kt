package com.villapark.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.villapark.app.presentation.auth.LoginScreen

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<ScreenType>(ScreenType.Login) }
    
    when (currentScreen) {
        ScreenType.Login -> LoginScreen(
            onTenantLogin = { currentScreen = ScreenType.TenantHome },
            onLandlordLogin = { currentScreen = ScreenType.LandlordDashboard }
        )
        ScreenType.TenantHome -> TestScreen(
            title = "Tenant Home",
            onBack = { currentScreen = ScreenType.Login }
        )
        ScreenType.LandlordDashboard -> TestScreen(
            title = "Landlord Dashboard",
            onBack = { currentScreen = ScreenType.Login }
        )
    }
}

enum class ScreenType {
    Login,
    TenantHome,
    LandlordDashboard
}

@Composable
fun TestScreen(
    title: String,
    onBack: () -> Unit
) {
    var clickCount by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "App is running correctly!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Button(onClick = { clickCount++ }) {
            Text("Clicked $clickCount times")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) {
            Text("Back to Login")
        }
    }
}
