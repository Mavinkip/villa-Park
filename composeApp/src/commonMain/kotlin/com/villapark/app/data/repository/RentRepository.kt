package com.villapark.app.data.repository

import com.villapark.app.data.models.IssueStatus
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.Payment
import com.villapark.app.data.models.PaymentStatus
import com.villapark.app.data.models.Tenant
import com.villapark.app.data.models.UnitStatus
import com.villapark.app.data.models.`Unit`  // Note the backticks if you're using Unit as a class name
import kotlinx.coroutines.flow.Flow

interface RentRepository {
    // Tenant methods
    suspend fun getTenant(tenantId: String): Result<Tenant>
    suspend fun getTenantByPhone(phoneNumber: String): Result<Tenant>

    // Unit methods
    suspend fun getUnit(unitId: String): Result<`Unit`>
    suspend fun getAllUnits(): Result<List<`Unit`>>
    suspend fun getUnitsByStatus(status: UnitStatus): Result<List<`Unit`>>

    // Payment methods
    suspend fun initiatePayment(tenantId: String, amount: Double, phoneNumber: String): Result<String>
    suspend fun observePaymentStatus(paymentId: String): Flow<PaymentStatus>
    suspend fun getPaymentHistory(tenantId: String): Result<List<Payment>>

    // Maintenance methods
    suspend fun reportIssue(issue: MaintenanceIssue): Result<String>
    suspend fun getMaintenanceIssues(tenantId: String): Result<List<MaintenanceIssue>>
    suspend fun getAllMaintenanceIssues(): Result<List<MaintenanceIssue>>
    suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Result<Boolean>
}