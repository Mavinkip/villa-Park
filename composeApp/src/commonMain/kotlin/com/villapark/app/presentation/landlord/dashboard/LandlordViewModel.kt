package com.villapark.app.presentation.landlord.dashboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.villapark.app.data.models.IssueStatus
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.RentalUnitStatus
import com.villapark.app.data.models.RentalUnit as RentalUnit
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LandlordState(
    val units: List<RentalUnit> = emptyList(),
    val maintenanceIssues: List<MaintenanceIssue> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val stats: DashboardStats = DashboardStats()
)

data class DashboardStats(
    val totalUnits: Int = 0,
    val occupiedUnits: Int = 0,
    val overduePayments: Int = 0,
    val openIssues: Int = 0,
    val monthlyRevenue: Double = 0.0
)

sealed class LandlordEvent {
    object LoadData : LandlordEvent()
    data class MarkIssueAsFixed(val issueId: String) : LandlordEvent()
    data class CallPlumber(val issueId: String) : LandlordEvent()
    data class SelectTab(val index: Int) : LandlordEvent()
    data class RefreshData(val force: Boolean = false) : LandlordEvent()
}

class LandlordViewModel(private val repository: RentRepository) : ScreenModel {

    private val _state = MutableStateFlow(LandlordState())
    val state: StateFlow<LandlordState> = _state.asStateFlow()

    init { loadData() }

    fun onEvent(event: LandlordEvent) {
        when (event) {
            LandlordEvent.LoadData -> loadData()
            is LandlordEvent.MarkIssueAsFixed -> markIssueAsFixed(event.issueId)
            is LandlordEvent.CallPlumber -> callPlumber(event.issueId)
            is LandlordEvent.SelectTab -> selectTab(event.index)
            is LandlordEvent.RefreshData -> loadData()
        }
    }

    fun loadData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val units: List<RentalUnit> = repository.getAllRentalUnits().getOrElse { emptyList() }
                val issues: List<MaintenanceIssue> = repository.getAllMaintenanceIssues().getOrElse { emptyList() }
                _state.update {
                    it.copy(
                        units = units,
                        maintenanceIssues = issues,
                        stats = calculateStats(units, issues),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    fun markIssueAsFixed(issueId: String) {
        screenModelScope.launch {
            repository.updateIssueStatus(issueId, IssueStatus.RESOLVED)
            loadData()
        }
    }

    private fun callPlumber(issueId: String) {
        screenModelScope.launch {
            repository.updateIssueStatus(issueId, IssueStatus.IN_PROGRESS)
            loadData()
        }
    }

    fun selectTab(index: Int) {
        _state.update { it.copy(selectedTab = index) }
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
