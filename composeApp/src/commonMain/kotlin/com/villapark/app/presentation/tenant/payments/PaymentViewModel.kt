package com.villapark.app.presentation.tenant.payments

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.villapark.app.data.models.Payment
import com.villapark.app.data.models.PaymentStatus
import com.villapark.app.data.models.PaymentType
import com.villapark.app.data.models.PropertyConfig
import com.villapark.app.data.repository.RentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PaymentFlowStage {
    object Form : PaymentFlowStage()
    object WaitingForPin : PaymentFlowStage()
    data class Success(val paymentId: String, val receiptNumber: String = "") : PaymentFlowStage()
    data class Failed(val message: String) : PaymentFlowStage()
}

data class PaymentUiState(
    val stage: PaymentFlowStage = PaymentFlowStage.Form,
    val isLoadingConfig: Boolean = false,
    val meterNumber: String = "",
    val amount: String = "",
    val phoneNumber: String = "",
    val meterError: String? = null,
    val amountError: String? = null,
    val phoneError: String? = null,
    val propertyConfig: PropertyConfig = PropertyConfig(),
    val completedPayment: Payment? = null
)

sealed class PaymentEvent {
    data class LoadConfig(val tenantId: String) : PaymentEvent()
    data class MeterNumberChanged(val value: String) : PaymentEvent()
    data class AmountChanged(val value: String) : PaymentEvent()
    data class PhoneChanged(val value: String) : PaymentEvent()
    data class Submit(val tenantId: String, val unitId: String, val paymentType: PaymentType) : PaymentEvent()
    object Retry : PaymentEvent()
}

class PaymentViewModel(
    private val repository: RentRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()
    
    private var currentPaymentId: String? = null
    
    fun onEvent(event: PaymentEvent) {
        when (event) {
            is PaymentEvent.LoadConfig -> loadConfig()
            is PaymentEvent.MeterNumberChanged -> updateMeterNumber(event.value)
            is PaymentEvent.AmountChanged -> updateAmount(event.value)
            is PaymentEvent.PhoneChanged -> updatePhone(event.value)
            is PaymentEvent.Submit -> submitPayment(event.tenantId, event.unitId, event.paymentType)
            is PaymentEvent.Retry -> retryPayment()
        }
    }
    
    private fun loadConfig() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingConfig = true) }
            val result = repository.getPropertyConfig()
            result.onSuccess { config ->
                _state.update { it.copy(propertyConfig = config, isLoadingConfig = false) }
            }.onFailure {
                _state.update { it.copy(isLoadingConfig = false) }
            }
        }
    }
    
    private fun updateMeterNumber(value: String) {
        _state.update { 
            it.copy(meterNumber = value, meterError = if (value.isNotBlank() && value.length < 6) "Invalid meter number" else null)
        }
    }
    
    private fun updateAmount(value: String) {
        val amount = value.toDoubleOrNull()
        _state.update {
            it.copy(
                amount = value,
                amountError = when {
                    value.isBlank() -> "Amount is required"
                    amount == null -> "Invalid amount"
                    amount <= 0 -> "Amount must be greater than 0"
                    else -> null
                }
            )
        }
    }
    
    private fun updatePhone(value: String) {
        val cleaned = value.replace(Regex("[^0-9]"), "")
        _state.update {
            it.copy(
                phoneNumber = value,
                phoneError = when {
                    cleaned.length != 10 -> "Enter valid 10-digit phone number"
                    !cleaned.startsWith("07") && !cleaned.startsWith("01") -> "Must start with 07 or 01"
                    else -> null
                }
            )
        }
    }
    
    private fun submitPayment(tenantId: String, unitId: String, paymentType: PaymentType) {
        val currentState = _state.value
        
        // Validate
        val amount = currentState.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.update { it.copy(amountError = "Enter valid amount") }
            return
        }
        
        val phoneCleaned = currentState.phoneNumber.replace(Regex("[^0-9]"), "")
        if (phoneCleaned.length != 10) {
            _state.update { it.copy(phoneError = "Enter valid phone number") }
            return
        }
        
        if (paymentType == PaymentType.KPLC_TOKEN && currentState.meterNumber.isBlank()) {
            _state.update { it.copy(meterError = "Meter number required") }
            return
        }
        
        _state.update { it.copy(stage = PaymentFlowStage.WaitingForPin) }
        
        screenModelScope.launch {
            val result = when (paymentType) {
                PaymentType.RENT -> repository.initiateRentPayment(tenantId, unitId, amount, phoneCleaned)
                PaymentType.KPLC_TOKEN -> repository.initiateKplcPayment(tenantId, currentState.meterNumber, amount, phoneCleaned)
                PaymentType.INTERNET -> repository.initiateInternetPayment(tenantId, amount, phoneCleaned, currentState.propertyConfig.internetTillNumber)
            }
            
            result.fold(
                onSuccess = { paymentId ->
                    currentPaymentId = paymentId
                    listenForCompletion(paymentId)
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            stage = PaymentFlowStage.Failed(error.message ?: "Payment initiation failed")
                        )
                    }
                }
            )
        }
    }
    
    private fun listenForCompletion(paymentId: String) {
        screenModelScope.launch {
            repository.observePaymentStatus(paymentId).collect { status ->
                when (status) {
                    PaymentStatus.COMPLETED -> {
                        // Fetch the completed payment details
                        val history = repository.getPaymentHistory("").getOrNull()
                        val payment = history?.find { it.id == paymentId }
                        _state.update { 
                            it.copy(
                                stage = PaymentFlowStage.Success(paymentId),
                                completedPayment = payment
                            )
                        }
                    }
                    PaymentStatus.FAILED, PaymentStatus.CANCELLED -> {
                        _state.update { 
                            it.copy(
                                stage = PaymentFlowStage.Failed("Payment ${status.name.lowercase()}")
                            )
                        }
                    }
                    else -> { /* Still waiting */ }
                }
            }
        }
    }
    
    private fun retryPayment() {
        _state.update { 
            it.copy(
                stage = PaymentFlowStage.Form,
                meterError = null,
                amountError = null,
                phoneError = null
            )
        }
    }
}
