package com.villapark.app.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.gitlive.firebase.functions.functions
import com.villapark.app.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.runCatching

class FirebaseRentRepository : RentRepository {

    private val db = Firebase.firestore
    private val functions = Firebase.functions

    // ── Tenants ───────────────────────────────────────────────────────────────

    override suspend fun getTenant(tenantId: String): Result<Tenant> = runCatching {
        val snapshot = db.collection("tenants").document(tenantId).get()
        snapshot.data<Tenant>() ?: error("Tenant not found")
    }

    override suspend fun getTenantByPhone(phoneNumber: String): Result<Tenant> = runCatching {
        val query = db.collection("tenants").where("phoneNumber", "==", phoneNumber)
        val snapshot = query.get()
        snapshot.documents.firstOrNull()?.data<Tenant>()
            ?: error("No tenant found for this phone number")
    }

    override suspend fun getTenantByEmail(email: String): Result<Tenant> = runCatching {
        val query = db.collection("tenants").where("email", "==", email)
        val snapshot = query.get()
        snapshot.documents.firstOrNull()?.data<Tenant>()
            ?: error("No tenant found for this email")
    }

    // ── Units ─────────────────────────────────────────────────────────────────

    override suspend fun getRentalUnit(unitId: String): Result<RentalUnit> = runCatching {
        val snapshot = db.collection("units").document(unitId).get()
        snapshot.data<RentalUnit>() ?: error("Unit not found")
    }

    override suspend fun getAllRentalUnits(): Result<List<RentalUnit>> = runCatching {
        val snapshot = db.collection("units").get()
        snapshot.documents.mapNotNull { it.data<RentalUnit>() }
    }

    override suspend fun getRentalUnitsByStatus(status: UnitStatus): Result<List<RentalUnit>> = runCatching {
        val query = db.collection("units").where("status", "==", status.name)
        val snapshot = query.get()
        snapshot.documents.mapNotNull { it.data<RentalUnit>() }
    }

    // ── Payments ──────────────────────────────────────────────────────────────

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
        (result.data() as Map<String, Any>)["paymentId"] as? String
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
        (result.data() as Map<String, Any>)["paymentId"] as? String
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
        (result.data() as Map<String, Any>)["paymentId"] as? String
            ?: error("No paymentId returned from Cloud Function")
    }

    override fun observePaymentStatus(paymentId: String): Flow<PaymentStatus> =
        db.collection("payments").document(paymentId).snapshots.map { snapshot ->
            val status = snapshot.get("status") as? String ?: "PENDING"
            runCatching { PaymentStatus.valueOf(status) }.getOrDefault(PaymentStatus.PENDING)
        }

    override suspend fun getPaymentHistory(tenantId: String): Result<List<Payment>> = runCatching {
        val query = db.collection("payments")
            .where("tenantId", "==", tenantId)
            .orderBy("timestamp", Direction.DESCENDING)
        val snapshot = query.get()
        snapshot.documents.mapNotNull { it.data<Payment>() }
    }

    // ── Maintenance ───────────────────────────────────────────────────────────

    override suspend fun reportIssue(issue: MaintenanceIssue): Result<String> = runCatching {
        val docRef = db.collection("maintenance").add(issue)
        docRef.id
    }

    override suspend fun getMaintenanceIssues(tenantId: String): Result<List<MaintenanceIssue>> = runCatching {
        val query = db.collection("maintenance")
            .where("tenantId", "==", tenantId)
            .orderBy("createdAt", Direction.DESCENDING)
        val snapshot = query.get()
        snapshot.documents.mapNotNull { it.data<MaintenanceIssue>() }
    }

    override suspend fun getAllMaintenanceIssues(): Result<List<MaintenanceIssue>> = runCatching {
        val query = db.collection("maintenance").orderBy("createdAt", Direction.DESCENDING)
        val snapshot = query.get()
        val allIssues = snapshot.documents.mapNotNull { it.data<MaintenanceIssue>() }
        // Filter out RESOLVED issues client-side
        allIssues.filter { it.status != IssueStatus.RESOLVED }
    }

    override suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Result<kotlin.Unit> = runCatching {
        db.collection("maintenance").document(issueId).update(mapOf("status" to status.name))
        kotlin.Unit
    }

    // ── Property config ───────────────────────────────────────────────────────

    override suspend fun getPropertyConfig(): Result<PropertyConfig> = runCatching {
        val snapshot = db.collection("config").document("property").get()
        snapshot.data<PropertyConfig>() ?: PropertyConfig()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

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
