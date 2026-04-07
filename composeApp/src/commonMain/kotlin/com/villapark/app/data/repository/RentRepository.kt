package com.villapark.app.data.repository

import com.villapark.app.data.models.*
import kotlinx.coroutines.flow.Flow

interface RentRepository {
    // Tenants
    suspend fun getTenant(tenantId: String): Result<Tenant>
    suspend fun getTenantByPhone(phoneNumber: String): Result<Tenant>
    suspend fun getTenantByEmail(email: String): Result<Tenant>
    
    // Rental Units
    suspend fun getRentalUnit(unitId: String): Result<RentalUnit>
    suspend fun getAllRentalUnits(): Result<List<RentalUnit>>
    suspend fun getRentalUnitsByStatus(status: UnitStatus): Result<List<RentalUnit>>
    
    // Payments
    suspend fun initiateRentPayment(
        tenantId: String,
        unitId: String,
        amount: Double,
        phoneNumber: String
    ): Result<String>
    
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
    
    fun observePaymentStatus(paymentId: String): Flow<PaymentStatus>
    suspend fun getPaymentHistory(tenantId: String): Result<List<Payment>>
    
    // Maintenance
    suspend fun reportIssue(issue: MaintenanceIssue): Result<String>
    suspend fun getMaintenanceIssues(tenantId: String): Result<List<MaintenanceIssue>>
    suspend fun getAllMaintenanceIssues(): Result<List<MaintenanceIssue>>
    suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Result<kotlin.Unit>
    
    // Property config
    suspend fun getPropertyConfig(): Result<PropertyConfig>
}
