package com.villapark.app.presentation.tenant.home

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
    val error: String? = null,
    val isPaymentInProgress: Boolean = false,
    val showPaymentDialog: Boolean = false,
    val paymentState: PaymentState = PaymentState.Idle
)

sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    data class Success(val amount: Double, val reference: String) : PaymentState()
    data class Failed(val error: String) : PaymentState()
}

sealed class TenantHomeEvent {
    data class LoadData(val tenantId: String) : TenantHomeEvent()
    data class PayRent(val tenantId: String, val amount: Double, val phoneNumber: String) : TenantHomeEvent()
    object DismissPaymentDialog : TenantHomeEvent()
    data class RefreshData(val tenantId: String) : TenantHomeEvent()
}

class TenantHomeViewModel(
    private val repository: RentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TenantHomeState())
    val state: StateFlow<TenantHomeState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TenantHomeEvent>()

    init {
        handleEvents()
    }

    fun onEvent(event: TenantHomeEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private fun handleEvents() {
        viewModelScope.launch {
            _events.collect { event ->
                when (event) {
                    is TenantHomeEvent.LoadData -> loadData(event.tenantId)
                    is TenantHomeEvent.PayRent -> payRent(event.tenantId, event.amount, event.phoneNumber)
                    is TenantHomeEvent.DismissPaymentDialog -> dismissPaymentDialog()
                    is TenantHomeEvent.RefreshData -> refreshData(event.tenantId)
                }
            }
        }
    }

    private suspend fun loadData(tenantId: String) {
        _state.update { it.copy(isLoading = true, error = null) }

        try {
            // Load tenant
            val tenantResult = repository.getTenant(tenantId)
            if (tenantResult.isFailure) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Failed to load tenant data"
                )}
                return
            }
            val tenant = tenantResult.getOrNull()

            // Load unit if tenant has one
            var unit: Unit? = null
            if (tenant?.unitId?.isNotEmpty() == true) {
                val unitResult = repository.getUnit(tenant.unitId)
                unit = unitResult.getOrNull()
            }

            // Load maintenance issues
            val issuesResult = repository.getMaintenanceIssues(tenantId)
            val issues: List<MaintenanceIssue> = issuesResult.getOrElse { emptyList() }

            // Load payment history
            val paymentsResult = repository.getPaymentHistory(tenantId)
            val payments: List<Payment> = paymentsResult.getOrElse { emptyList() } // FIXED: Now correctly typed as List<Payment>

            _state.update {
                it.copy(
                    tenant = tenant,
                    unit = unit,
                    maintenanceIssues = issues.filter { issue ->
                        issue.status != IssueStatus.RESOLVED
                    },
                    recentPayments = payments.take(5), // Now payments is correctly typed as List<Payment>
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

    private suspend fun payRent(tenantId: String, amount: Double, phoneNumber: String) {
        _state.update {
            it.copy(
                isPaymentInProgress = true,
                showPaymentDialog = true,
                paymentState = PaymentState.Processing
            )
        }

        try {
            val result = repository.initiatePayment(tenantId, amount, phoneNumber)

            result.fold(
                onSuccess = { paymentId ->
                    // Observe payment status
                    repository.observePaymentStatus(paymentId)
                        .onEach { status ->
                            when (status) {
                                PaymentStatus.COMPLETED -> {
                                    _state.update {
                                        it.copy(
                                            isPaymentInProgress = false,
                                            paymentState = PaymentState.Success(
                                                amount = amount,
                                                reference = paymentId
                                            )
                                        )
                                    }
                                    // Refresh data
                                    loadData(tenantId)
                                }
                                PaymentStatus.FAILED -> {
                                    _state.update {
                                        it.copy(
                                            isPaymentInProgress = false,
                                            paymentState = PaymentState.Failed("Payment failed. Please try again.")
                                        )
                                    }
                                }
                                else -> { /* Still pending */ }
                            }
                        }
                        .launchIn(viewModelScope)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isPaymentInProgress = false,
                            paymentState = PaymentState.Failed(error.message ?: "Payment initiation failed")
                        )
                    }
                }
            )
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isPaymentInProgress = false,
                    paymentState = PaymentState.Failed("Error: ${e.message}")
                )
            }
        }
    }

    private fun dismissPaymentDialog() {
        _state.update {
            it.copy(
                showPaymentDialog = false,
                paymentState = PaymentState.Idle
            )
        }
    }

    private suspend fun refreshData(tenantId: String) {
        loadData(tenantId)
    }
}