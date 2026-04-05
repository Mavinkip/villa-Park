package com.villapark.app.presentation.tenant.issues

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/presentation/tenant/issues/IssueReportViewModel.kt
// ACTION: REPLACE entire file (was empty)

import androidx.lifecycle.ViewModel
import com.villapark.app.data.repository.RentRepository

// Stub ViewModel so Koin can inject it and the app compiles.
// Full implementation comes in Session 6 when we build the Issue Report screen.
class IssueReportViewModel(private val repository: RentRepository) : ViewModel()