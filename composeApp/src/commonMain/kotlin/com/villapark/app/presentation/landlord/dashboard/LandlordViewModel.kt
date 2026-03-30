package com.villapark.app.presentation.landlord.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.villapark.app.data.models.*
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LandlordState(
    val units: List<Unit> = emptyList(),
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

class LandlordViewModel(
    private val repository: RentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LandlordState())
    val state: StateFlow<LandlordState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LandlordEvent>()

    init {
        handleEvents()
        loadData()
    }

    fun onEvent(event: LandlordEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun handleEvents() {
        viewModelScope.launch {
            _events.collect { event ->
                when (event) {
                    LandlordEvent.LoadData -> loadData()
                    is LandlordEvent.MarkIssueAsFixed -> markIssueAsFixed(event.issueId)
                    is LandlordEvent.CallPlumber -> callPlumber(event.issueId)
                    is LandlordEvent.SelectTab -> selectTab(event.index)
                    is LandlordEvent.RefreshData -> refreshData(event.force)
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Load units
                val unitsResult = repository.getAllUnits()
                val units = unitsResult.getOrElse { emptyList() }

                // Load maintenance issues
                val issuesResult = repository.getAllMaintenanceIssues()
                val issues = issuesResult.getOrElse { emptyList() }

                // Calculate stats
                val stats = calculateStats(units, issues)

                _state.update {
                    it.copy(
                        units = units,
                        maintenanceIssues = issues,
                        stats = stats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading data: ${e.message}"
                    )
                }
            }
        }
    }

    fun markIssueAsFixed(issueId: String) {
        viewModelScope.launch {
            try {
                repository.updateIssueStatus(issueId, IssueStatus.RESOLVED)
                // Refresh data
                loadData()
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Failed to update issue: ${e.message}")
                }
            }
        }
    }

    private fun callPlumber(issueId: String) {
        // This would integrate with phone/calling functionality
        // For now, just mark as in progress
        viewModelScope.launch {
            try {
                repository.updateIssueStatus(issueId, IssueStatus.IN_PROGRESS)
                loadData()
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Failed to update issue: ${e.message}")
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _state.update { it.copy(selectedTab = index) }
    }

    private fun refreshData(force: Boolean) {
        loadData()
    }

    private fun calculateStats(units: List<Unit>, issues: List<MaintenanceIssue>): DashboardStats {
        val totalUnits = units.size
        val occupiedUnits = units.count { it.status != UnitStatus.VACANT }
        val overduePayments = units.count { it.status == UnitStatus.OCCUPIED_OVERDUE }
        val openIssues = issues.count { it.status != IssueStatus.RESOLVED }
        val monthlyRevenue = units
            .filter { it.status == UnitStatus.OCCUPIED_PAID }
            .sumOf { it.rentAmount }

        return DashboardStats(
            totalUnits = totalUnits,
            occupiedUnits = occupiedUnits,
            overduePayments = overduePayments,
            openIssues = openIssues,
            monthlyRevenue = monthlyRevenue
        )
    }
}