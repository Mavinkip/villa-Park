package com.villapark.app.data.repository

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/data/repository/RentRepository.kt
// ACTION: REPLACE entire file

import com.villapark.app.data.models.IssueStatus
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.Payment
import com.villapark.app.data.models.PaymentStatus
import com.villapark.app.data.models.PaymentType
import com.villapark.app.data.models.Tenant
import com.villapark.app.data.models.Unit
import com.villapark.app.data.models.UnitStatus
import kotlinx.coroutines.flow.Flow

interface RentRepository {

    // ── Tenants ───────────────────────────────────────────────────────────────
    suspend fun getTenant(tenantId: String): Result<Tenant>
    suspend fun getTenantByPhone(phoneNumber: String): Result<Tenant>
    suspend fun getTenantByEmail(email: String): Result<Tenant>

    // ── Units ─────────────────────────────────────────────────────────────────
    suspend fun getUnit(unitId: String): Result<Unit>
    suspend fun getAllUnits(): Result<List<Unit>>
    suspend fun getUnitsByStatus(status: UnitStatus): Result<List<Unit>>

    // ── Payments ──────────────────────────────────────────────────────────────
    // All three call Firebase Cloud Functions, which call PayHero's API.
    // The app never calls PayHero directly — API username/password stays on server.
    suspend fun initiateRentPayment(
        tenantId: String,
        unitId: String,
        amount: Double,
        phoneNumber: String
    ): Result<String>  // returns paymentId (Firestore doc ID)

    suspend fun initiateKplcPayment(
        tenantId: String,
        meterNumber: String,
        amount: Double,
        phoneNumber: String
    ): Result<String>

    suspend fun initiateInternetPayment(
        tenantId: String,
        amount: Double,
        phoneNumber: String,
        tillNumber: String
    ): Result<String>

    // Real-time Firestore listener — NOT suspend, returns a live Flow
    fun observePaymentStatus(paymentId: String): Flow<PaymentStatus>

    suspend fun getPaymentHistory(tenantId: String): Result<List<Payment>>

    // ── Maintenance ───────────────────────────────────────────────────────────
    suspend fun reportIssue(issue: MaintenanceIssue): Result<String>
    suspend fun getMaintenanceIssues(tenantId: String): Result<List<MaintenanceIssue>>
    suspend fun getAllMaintenanceIssues(): Result<List<MaintenanceIssue>>
    suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Result<Unit>

    // ── Property config ───────────────────────────────────────────────────────
    suspend fun getPropertyConfig(): Result<PropertyConfig>
}

// Set once by the landlord in Firestore at path "config/property"
data class PropertyConfig(
    val id: String = "",
    val name: String = "Villa Park",
    val rentPaybill: String = "",           // Your rent paybill number
    val rentAccountPrefix: String = "UNIT", // Account = "UNIT-4A"
    val internetTillNumber: String = "",    // Your internet till number
    val payHeroChannelId: Int = 0,          // From PayHero dashboard → Payment Channels
    val kplcPaybill: String = "888880"      // KPLC official paybill — rarely changes
)