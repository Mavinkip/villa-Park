package com.villapark.app.data.repository

import com.villapark.app.data.models.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class FirebaseRentRepository : RentRepository {

    private val firestore = Firebase.firestore
    private val functions = Firebase.functions

    override suspend fun getTenant(tenantId: String): Result<Tenant> = try {
        val document = firestore.collection("tenants")
            .document(tenantId)
            .get()

        val tenant = document.data<Tenant>()
        Result.success(tenant ?: throw Exception("Tenant not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTenantByPhone(phoneNumber: String): Result<Tenant> = try {
        val query = firestore.collection("tenants")
            .whereEqualTo("phoneNumber", phoneNumber)
            .get()

        val tenant = query.documents.firstOrNull()?.data<Tenant>()
        Result.success(tenant ?: throw Exception("Tenant not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUnit(unitId: String): Result<Unit> = try {
        val document = firestore.collection("units")
            .document(unitId)
            .get()

        val unit = document.data<Unit>()
        Result.success(unit ?: throw Exception("Unit not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAllUnits(): Result<List<Unit>> = try {
        val snapshot = firestore.collection("units")
            .get()

        val units = snapshot.documents.mapNotNull { it.data<Unit>() }
        Result.success(units)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUnitsByStatus(status: UnitStatus): Result<List<Unit>> = try {
        val snapshot = firestore.collection("units")
            .whereEqualTo("status", status.name)
            .get()

        val units = snapshot.documents.mapNotNull { it.data<Unit>() }
        Result.success(units)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun initiatePayment(
        tenantId: String,
        amount: Double,
        phoneNumber: String
    ): Result<String> = try {
        val result = functions.httpsCallable("initiateSTKPush")
            .invoke(mapOf(
                "tenantId" to tenantId,
                "amount" to amount,
                "phoneNumber" to phoneNumber
            ))

        val paymentId = result.data<String>()
        Result.success(paymentId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun observePaymentStatus(paymentId: String): Flow<PaymentStatus> = flow {
        val docRef = firestore.collection("payments").document(paymentId)

        docRef.snapshots.collect { snapshot ->
            val status = snapshot.get<String>("status")?.let {
                PaymentStatus.valueOf(it)
            } ?: PaymentStatus.PENDING
            emit(status)
        }
    }.catch { e ->
        // Handle errors
    }

    override suspend fun getPaymentHistory(tenantId: String): Result<List<Payment>> = try {
        val snapshot = firestore.collection("payments")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("timestamp", true) // true for descending
            .get()

        val payments = snapshot.documents.mapNotNull { it.data<Payment>() }
        Result.success(payments)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getMaintenanceIssues(tenantId: String): Result<List<MaintenanceIssue>> = try {
        val snapshot = firestore.collection("maintenance")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("createdAt", true)
            .get()

        val issues = snapshot.documents.mapNotNull { it.data<MaintenanceIssue>() }
        Result.success(issues)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun reportIssue(issue: MaintenanceIssue): Result<String> = try {
        val docRef = firestore.collection("maintenance")
            .add(issue)

        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAllMaintenanceIssues(): Result<List<MaintenanceIssue>> = try {
        val snapshot = firestore.collection("maintenance")
            .whereNotEqualTo("status", IssueStatus.RESOLVED.name)
            .orderBy("createdAt", true)
            .get()

        val issues = snapshot.documents.mapNotNull { it.data<MaintenanceIssue>() }
        Result.success(issues)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Result<Unit> = try {
        firestore.collection("maintenance")
            .document(issueId)
            .update(mapOf("status" to status.name))

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}