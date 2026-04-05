package com.villapark.app.data.repository

// FILE: composeApp/src/commonMain/kotlin/com/villapark/app/data/repository/FirebaseRentRepository.kt
// ACTION: REPLACE entire file

import com.villapark.app.data.models.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseRentRepository : RentRepository {

    private val db = Firebase.firestore
    private val functions = Firebase.functions

    // ── Tenants ───────────────────────────────────────────────────────────────

    override suspend fun getTenant(tenantId: String): Result<Tenant> = runCatching {
        db.collection("tenants").document(tenantId).get().data<Tenant>()
            ?: error("Tenant not found")
    }

    override suspend fun getTenantByPhone(phoneNumber: String): Result<Tenant> = runCatching {
        db.collection("tenants").whereEqualTo("phoneNumber", phoneNumber)
            .get().documents.firstOrNull()?.data<Tenant>()
            ?: error("No tenant found for this phone number")
    }

    override suspend fun getTenantByEmail(email: String): Result<Tenant> = runCatching {
        db.collection("tenants").whereEqualTo("email", email)
            .get().documents.firstOrNull()?.data<Tenant>()
            ?: error("No tenant found for this email")
    }

    // ── Units ─────────────────────────────────────────────────────────────────

    override suspend fun getUnit(unitId: String): Result<Unit> = runCatching {
        db.collection("units").document(unitId).get().data<Unit>()
            ?: error("Unit not found")
    }

    override suspend fun getAllUnits(): Result<List<Unit>> = runCatching {
        db.collection("units").get().documents.mapNotNull { it.data<Unit>() }
    }

    override suspend fun getUnitsByStatus(status: UnitStatus): Result<List<Unit>> = runCatching {
        db.collection("units").whereEqualTo("status", status.name)
            .get().documents.mapNotNull { it.data<Unit>() }
    }

    // ── Payments ──────────────────────────────────────────────────────────────
    // HOW PAYHERO WORKS (simpler than Daraja):
    //   1. App calls Cloud Function with amount + phone + type
    //   2. Cloud Function calls PayHero API (POST /api/v2/payments) with Basic Auth
    //   3. PayHero sends STK push to tenant's phone
    //   4. Tenant enters PIN
    //   5. PayHero sends callback to our Cloud Function URL
    //   6. Cloud Function updates Firestore payment doc → status = COMPLETED
    //   7. Our Flow fires, UI shows success
    //
    // PayHero only needs: username, password, channel_id — no token refresh needed!

    override suspend fun initiateRentPayment(
        tenantId: String,
        unitId: String,
        amount: Double,
        phoneNumber: String
    ): Result<String> = runCatching {
        val result = functions.httpsCallable("initiateRentPayment").invoke(
            mapOf(
                "tenantId" to tenantId,
                "unitId" to unitId,
                "amount" to amount,
                "phoneNumber" to formatPhone(phoneNumber)
            )
        )
        @Suppress("UNCHECKED_CAST")
        (result.data as Map<String, Any>)["paymentId"] as? String
            ?: error("No paymentId returned from Cloud Function")
    }

    override suspend fun initiateKplcPayment(
        tenantId: String,
        meterNumber: String,
        amount: Double,
        phoneNumber: String
    ): Result<String> = runCatching {
        val result = functions.httpsCallable("initiateKplcPayment").invoke(
            mapOf(
                "tenantId" to tenantId,
                "meterNumber" to meterNumber.trim(),
                "amount" to amount,
                "phoneNumber" to formatPhone(phoneNumber)
            )
        )
        @Suppress("UNCHECKED_CAST")
        (result.data as Map<String, Any>)["paymentId"] as? String
            ?: error("No paymentId returned from Cloud Function")
    }

    override suspend fun initiateInternetPayment(
        tenantId: String,
        amount: Double,
        phoneNumber: String,
        tillNumber: String
    ): Result<String> = runCatching {
        val result = functions.httpsCallable("initiateInternetPayment").invoke(
            mapOf(
                "tenantId" to tenantId,
                "amount" to amount,
                "phoneNumber" to formatPhone(phoneNumber),
                "tillNumber" to tillNumber
            )
        )
        @Suppress("UNCHECKED_CAST")
        (result.data as Map<String, Any>)["paymentId"] as? String
            ?: error("No paymentId returned from Cloud Function")
    }

    override fun observePaymentStatus(paymentId: String): Flow<PaymentStatus> =
        db.collection("payments").document(paymentId).snapshots.map { snap ->
            val s = snap.get<String>("status") ?: "PENDING"
            runCatching { PaymentStatus.valueOf(s) }.getOrDefault(PaymentStatus.PENDING)
        }

    override suspend fun getPaymentHistory(tenantId: String): Result<List<Payment>> = runCatching {
        db.collection("payments")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("timestamp", Direction.DESCENDING)
            .get().documents.mapNotNull { it.data<Payment>() }
    }

    // ── Maintenance ───────────────────────────────────────────────────────────

    override suspend fun reportIssue(issue: MaintenanceIssue): Result<String> = runCatching {
        db.collection("maintenance").add(issue).id
    }

    override suspend fun getMaintenanceIssues(tenantId: String): Result<List<MaintenanceIssue>> = runCatching {
        db.collection("maintenance")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("createdAt", Direction.DESCENDING)
            .get().documents.mapNotNull { it.data<MaintenanceIssue>() }
    }

    override suspend fun getAllMaintenanceIssues(): Result<List<MaintenanceIssue>> = runCatching {
        db.collection("maintenance")
            .whereNotEqualTo("status", IssueStatus.RESOLVED.name)
            .orderBy("createdAt", Direction.DESCENDING)
            .get().documents.mapNotNull { it.data<MaintenanceIssue>() }
    }

    // FIXED: return type is Result<Unit> — was Result<Boolean> in original code
    override suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Result<Unit> = runCatching {
        db.collection("maintenance").document(issueId).update(mapOf("status" to status.name))
    }

    // ── Property config ───────────────────────────────────────────────────────

    override suspend fun getPropertyConfig(): Result<PropertyConfig> = runCatching {
        db.collection("config").document("property").get().data<PropertyConfig>()
            ?: PropertyConfig()
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    // PayHero accepts Kenyan numbers in 07XXXXXXXX format (unlike Daraja which needs 254...)
    // This normalises all input formats to 07XXXXXXXX
    private fun formatPhone(phone: String): String {
        val cleaned = phone.trim().replace(" ", "").replace("-", "")
        return when {
            cleaned.startsWith("+254") -> "0${cleaned.removePrefix("+254")}"
            cleaned.startsWith("254")  -> "0${cleaned.removePrefix("254")}"
            cleaned.startsWith("07") || cleaned.startsWith("01") -> cleaned
            else -> "0$cleaned"
        }
    }
}