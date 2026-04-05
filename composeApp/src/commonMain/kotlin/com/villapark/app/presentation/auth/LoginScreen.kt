package com.villapark.app.presentation.auth

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/presentation/auth/LoginScreen.kt
// ACTION: REPLACE entire file

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreenContent(
    onTenantLogin: (tenantId: String) -> Unit,
    onLandlordLogin: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 72.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏠", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Villa Park", style = MaterialTheme.typography.headlineLarge)
            Text("Property Management", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("I'm a Tenant") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("I'm a Landlord") })
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (selectedTab) {
            0 -> TenantLoginForm(onLogin = onTenantLogin)
            1 -> LandlordLoginForm(onLogin = onLandlordLogin)
        }
    }
}

@Composable
private fun TenantLoginForm(onLogin: (tenantId: String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Enter your phone number to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; error = null },
            label = { Text("Phone Number") },
            placeholder = { Text("e.g. 0712 345 678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        Button(
            onClick = {
                val cleaned = phone.trim().replace(" ", "")
                if (cleaned.length < 10) {
                    error = "Enter a valid Kenyan phone number"
                } else {
                    // TODO Session 5: add Firebase Phone Auth OTP verification
                    onLogin(cleaned)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = phone.isNotBlank()
        ) {
            Text("Continue")
        }

        Text("Your phone number is used to find your unit and balance.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LandlordLoginForm(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    error = "Please fill in all fields"
                } else {
                    // TODO Session 5: Firebase email/password auth
                    onLogin()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign In") }
    }
}