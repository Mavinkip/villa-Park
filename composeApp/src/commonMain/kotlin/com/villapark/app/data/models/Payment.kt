package com.villapark.app.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val id: String = "",
    val tenantId: String = "",
    val unitId: String = "",
    val amount: Double = 0.0,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val mpesaReference: String = "",
    val  mpesaReceipt: String = "",
    val phoneNumber: String = "",
    val timestamp: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,

)
enum class PaymentStatus {
    PENDING,COMPLETED, FAILED
}



