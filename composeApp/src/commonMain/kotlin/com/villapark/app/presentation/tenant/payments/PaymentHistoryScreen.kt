package com.villapark.app.presentation.tenant.payments

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.villapark.app.data.repository.FirebaseRentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    tenantId: String,
    repository: FirebaseRentRepository,
    onBack: () -> Unit,
    tenatId: String
){

}