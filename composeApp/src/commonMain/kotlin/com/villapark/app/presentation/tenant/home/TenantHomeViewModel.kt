package com.villapark.app.presentation.tenant.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.RentalUnit
import com.villapark.app.data.models.Tenant
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class TenantHomeState(
    val tenant: Tenant? = null,
    val unit: RentalUnit? = null,
    val maintenanceIssues: List<MaintenanceIssue> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TenantHomeEvent {
    data class LoadData(val tenantId: String) : TenantHomeEvent()
    data class RefreshData(val tenantId: String) : TenantHomeEvent()
}

class TenantHomeViewModel : ScreenModel, KoinComponent {
    private val repository: RentRepository by inject()
    
    private val _state = MutableStateFlow(TenantHomeState())
    val state: StateFlow<TenantHomeState> = _state.asStateFlow()
    
    fun onEvent(event: TenantHomeEvent) {
        when (event) {
            is TenantHomeEvent.LoadData -> loadData(event.tenantId)
            is TenantHomeEvent.RefreshData -> refreshData(event.tenantId)
        }
    }
    
    private fun loadData(tenantId: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val tenantResult = repository.getTenant(tenantId)
            val tenant = tenantResult.getOrNull()
            
            val unit = if (tenant != null && tenant.unitId.isNotEmpty()) {
                repository.getRentalUnit(tenant.unitId).getOrNull()
            } else null
            
            val issues = if (tenant != null) {
                repository.getMaintenanceIssues(tenantId).getOrNull() ?: emptyList()
            } else emptyList()
            
            _state.update {
                it.copy(
                    tenant = tenant,
                    unit = unit,
                    maintenanceIssues = issues,
                    isLoading = false,
                    error = if (tenant == null) "Failed to load tenant data" else null
                )
            }
        }
    }
    
    private fun refreshData(tenantId: String) {
        loadData(tenantId)
    }
}
