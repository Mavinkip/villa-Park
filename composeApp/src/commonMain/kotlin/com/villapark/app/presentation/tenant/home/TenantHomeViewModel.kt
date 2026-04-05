package com.villapark.app.presentation.tenant.home

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/presentation/tenant/home/TenantHomeViewModel.kt
// ACTION: REPLACE entire file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.villapark.app.data.models.*
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TenantHomeState(
    val tenant: Tenant? = null,
    val unit: Unit? = null,
    val maintenanceIssues: List<MaintenanceIssue> = emptyList(),
    val recentPayments: List<Payment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TenantHomeEvent {
    data class LoadData(val tenantId: String) : TenantHomeEvent()
    data class RefreshData(val tenantId: String) : TenantHomeEvent()
}

class TenantHomeViewModel(private val repository: RentRepository) : ViewModel() {

    private val _state = MutableStateFlow(TenantHomeState())
    val state: StateFlow<TenantHomeState> = _state.asStateFlow()

    fun onEvent(event: TenantHomeEvent) {
        when (event) {
            is TenantHomeEvent.LoadData -> loadData(event.tenantId)
            is TenantHomeEvent.RefreshData -> loadData(event.tenantId)
        }
    }

    private fun loadData(tenantId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val tenant = repository.getTenant(tenantId).getOrNull()
                val unit = if (tenant?.unitId?.isNotEmpty() == true)
                    repository.getUnit(tenant.unitId).getOrNull() else null
                val issues = repository.getMaintenanceIssues(tenantId)
                    .getOrElse { emptyList() }
                    .filter { it.status != IssueStatus.RESOLVED }
                val payments = repository.getPaymentHistory(tenantId).getOrElse { emptyList() }

                _state.update {
                    it.copy(
                        tenant = tenant, unit = unit,
                        maintenanceIssues = issues,
                        recentPayments = payments.take(5),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }
}