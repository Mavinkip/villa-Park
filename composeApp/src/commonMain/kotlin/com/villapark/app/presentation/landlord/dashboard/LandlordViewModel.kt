package com.villapark.app.presentation.landlord.dashboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.villapark.app.data.models.IssueStatus
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.RentalUnit
import com.villapark.app.data.models.UnitStatus
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalUnits: Int = 0,
    val occupiedUnits: Int = 0,
    val overduePayments: Int = 0,
    val openIssues: Int = 0,
    val monthlyRevenue: Double = 0.0
)

data class LandlordState(
    val units: List<RentalUnit> = emptyList(),
    val maintenanceIssues: List<MaintenanceIssue> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val isLoading: Boolean = false,
    val selectedTab: Int = 0,
    val error: String? = null
)

sealed class LandlordEvent {
    data class RefreshData(val force: Boolean = false) : LandlordEvent()
    data class SelectTab(val tab: Int) : LandlordEvent()
    data class MarkIssueAsFixed(val issueId: String) : LandlordEvent()
    data class CallPlumber(val issueId: String) : LandlordEvent()
}

class LandlordViewModel(
    private val repository: RentRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(LandlordState())
    val state: StateFlow<LandlordState> = _state.asStateFlow()
    
    fun onEvent(event: LandlordEvent) {
        when (event) {
            is LandlordEvent.RefreshData -> loadData()
            is LandlordEvent.SelectTab -> selectTab(event.tab)
            is LandlordEvent.MarkIssueAsFixed -> markIssueAsFixed(event.issueId)
            is LandlordEvent.CallPlumber -> callPlumber(event.issueId)
        }
    }
    
    fun selectTab(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }
    
    fun markIssueAsFixed(issueId: String) {
        screenModelScope.launch {
            repository.updateIssueStatus(issueId, IssueStatus.RESOLVED)
            loadData()
        }
    }
    
    fun callPlumber(issueId: String) {
        // TODO: Implement call plumber functionality
    }
    
    fun loadData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val unitsResult = repository.getAllRentalUnits()
            val units = unitsResult.getOrNull() ?: emptyList()
            
            val issuesResult = repository.getAllMaintenanceIssues()
            val issues = issuesResult.getOrNull() ?: emptyList()
            
            _state.update {
                it.copy(
                    units = units,
                    maintenanceIssues = issues,
                    stats = calculateStats(units, issues),
                    isLoading = false,
                    error = if (units.isEmpty() && issues.isEmpty()) "Failed to load data" else null
                )
            }
        }
    }
    
    private fun calculateStats(
        units: List<RentalUnit>,
        issues: List<MaintenanceIssue>
    ) = DashboardStats(
        totalUnits = units.size,
        occupiedUnits = units.count { it.status != UnitStatus.VACANT },
        overduePayments = units.count { it.status == UnitStatus.OCCUPIED_OVERDUE },
        openIssues = issues.count { it.status != IssueStatus.RESOLVED },
        monthlyRevenue = units.filter { it.status == UnitStatus.OCCUPIED_PAID }.sumOf { it.rentAmount }
    )
}
