package com.villapark.app.data.models

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/data/models/Payment.kt
// ACTION: REPLACE entire file

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

// WHY THREE TYPES:
// RENT        → STK push to your paybill number
// KPLC_TOKEN  → STK push to KPLC paybill 888880, account = meter number
// INTERNET    → STK push to your till number
enum class PaymentType {
    RENT,
    KPLC_TOKEN,
    INTERNET
}

@Serializable
data class Payment(
    val id: String = "",
    val tenantId: String = "",
    val unitId: String = "",
    val type: PaymentType = PaymentType.RENT,
    val amount: Double = 0.0,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val phoneNumber: String = "",

    // PayHero response fields — filled by Cloud Function after PayHero responds
    val payHeroReference: String = "",       // PayHero's "reference" e.g. "E8UWT7CLUW"
    val checkoutRequestId: String = "",      // PayHero's "CheckoutRequestID"
    val mpesaReceiptNumber: String = "",     // M-Pesa receipt after completion e.g. "QKA4MXYZ12"

    // KPLC only — filled after payment completes
    val meterNumber: String = "",
    val kplcToken: String = "",              // e.g. "1234-5678-9012-3456-7890"
    val kplcUnits: Double = 0.0,

    // Internet bill only
    val tillNumber: String = "",

    val timestamp: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null
)

enum class PaymentStatus {
    PENDING,    // STK push sent, waiting for tenant PIN
    COMPLETED,  // PayHero confirmed via callback
    FAILED,     // Wrong PIN, insufficient funds, timeout
    CANCELLED   // Tenant dismissed the STK prompt
}