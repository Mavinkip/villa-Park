package com.villapark.app.presentation.tenant.payments

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.villapark.app.data.models.Payment
import com.villapark.app.data.models.PaymentStatus
import com.villapark.app.data.models.PaymentType
import com.villapark.app.data.repository.PropertyConfig
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaymentUiState(
    val isLoadingConfig: Boolean = false,
    val propertyConfig: PropertyConfig = PropertyConfig(),
    val phoneNumber: String = "",
    val amount: String = "",
    val meterNumber: String = "",
    val phoneError: String? = null,
    val amountError: String? = null,
    val meterError: String? = null,
    val stage: PaymentFlowStage = PaymentFlowStage.Form,
    val completedPayment: Payment? = null
)

sealed class PaymentFlowStage {
    object Form : PaymentFlowStage()
    object WaitingForPin : PaymentFlowStage()
    object Success : PaymentFlowStage()
    data class Failed(val message: String) : PaymentFlowStage()
}

sealed class PaymentEvent {
    data class LoadConfig(val tenantId: String) : PaymentEvent()
    data class PhoneChanged(val value: String) : PaymentEvent()
    data class AmountChanged(val value: String) : PaymentEvent()
    data class MeterNumberChanged(val value: String) : PaymentEvent()
    data class Submit(val tenantId: String, val unitId: String, val type: PaymentType) : PaymentEvent()
    object Retry : PaymentEvent()
}

class PaymentViewModel(private val repository: RentRepository) : ScreenModel {

    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    fun onEvent(event: PaymentEvent) {
        when (event) {
            is PaymentEvent.LoadConfig -> loadConfig(event.tenantId)
            is PaymentEvent.PhoneChanged -> _state.update { it.copy(phoneNumber = event.value, phoneError = null) }
            is PaymentEvent.AmountChanged -> _state.update { it.copy(amount = event.value, amountError = null) }
            is PaymentEvent.MeterNumberChanged -> _state.update { it.copy(meterNumber = event.value, meterError = null) }
            is PaymentEvent.Submit -> submit(event.tenantId, event.unitId, event.type)
            is PaymentEvent.Retry -> _state.update { it.copy(stage = PaymentFlowStage.Form) }
        }
    }

    private fun loadConfig(tenantId: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingConfig = true) }
            repository.getPropertyConfig().onSuccess { config ->
                _state.update { it.copy(propertyConfig = config, isLoadingConfig = false) }
            }.onFailure {
                _state.update { it.copy(isLoadingConfig = false) }
            }
        }
    }

    private fun submit(tenantId: String, unitId: String, type: PaymentType) {
        val s = _state.value
        val amountDouble = s.amount.toDoubleOrNull()
        var hasError = false

        if (s.phoneNumber.trim().length < 10) {
            _state.update { it.copy(phoneError = "Enter a valid Kenyan phone number") }
            hasError = true
        }
        if (amountDouble == null || amountDouble < 1) {
            _state.update { it.copy(amountError = "Enter a valid amount (minimum KSh 1)") }
            hasError = true
        }
        if (type == PaymentType.KPLC_TOKEN && s.meterNumber.trim().length < 6) {
            _state.update { it.copy(meterError = "Enter your meter number") }
            hasError = true
        }
        if (hasError) return

        screenModelScope.launch {
            _state.update { it.copy(stage = PaymentFlowStage.WaitingForPin) }

            val result = when (type) {
                PaymentType.RENT -> repository.initiateRentPayment(
                    tenantId, unitId, amountDouble!!, s.phoneNumber
                )
                PaymentType.KPLC_TOKEN -> repository.initiateKplcPayment(
                    tenantId, s.meterNumber, amountDouble!!, s.phoneNumber
                )
                PaymentType.INTERNET -> repository.initiateInternetPayment(
                    tenantId, amountDouble!!, s.phoneNumber, s.propertyConfig.internetTillNumber
                )
            }

            result.fold(
                onSuccess = { paymentId -> listenForCompletion(paymentId, tenantId) },
                onFailure = { error ->
                    _state.update {
                        it.copy(stage = PaymentFlowStage.Failed(
                            error.message ?: "Could not connect to payment service."
                        ))
                    }
                }
            )
        }
    }

    private fun listenForCompletion(paymentId: String, tenantId: String) {
        repository.observePaymentStatus(paymentId)
            .onEach { status ->
                when (status) {
                    PaymentStatus.COMPLETED -> {
                        val payment = repository.getPaymentHistory(tenantId)
                            .getOrElse { emptyList() }
                            .firstOrNull { it.id == paymentId }
                        _state.update { it.copy(stage = PaymentFlowStage.Success, completedPayment = payment) }
                    }
                    PaymentStatus.FAILED -> _state.update {
                        it.copy(stage = PaymentFlowStage.Failed(
                            "Payment failed. Wrong PIN or insufficient M-Pesa balance."
                        ))
                    }
                    PaymentStatus.CANCELLED -> _state.update {
                        it.copy(stage = PaymentFlowStage.Failed(
                            "You cancelled the M-Pesa prompt. Tap Try Again to retry."
                        ))
                    }
                    PaymentStatus.PENDING -> { /* still waiting */ }
                }
            }
            .catch {
                _state.update {
                    it.copy(stage = PaymentFlowStage.Failed(
                        "Connection lost. Check your payment history to see if it went through."
                    ))
                }
            }
            .launchIn(screenModelScope)
    }
}
