package com.villapark.app.presentation.tenant.issues

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IssueReportState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class IssueReportViewModel(
    private val repository: RentRepository
) : ScreenModel {
    private val _state = MutableStateFlow(IssueReportState())
    val state: StateFlow<IssueReportState> = _state.asStateFlow()
    
    // TODO: Implement issue reporting functionality in Session 6
}
