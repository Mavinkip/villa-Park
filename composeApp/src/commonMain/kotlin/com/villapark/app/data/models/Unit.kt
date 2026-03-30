package com.villapark.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Unit(
    val id: String="",
    val number: String="",
    val status: UnitStatus = UnitStatus.VACANT,
    val tenantId: String? = null,
    val rentAmount: Double = 0.0,
    val bedrooms: Int = 1,
    val bathrooms: Int = 1,
    val utilities: Utilities = Utilities(),
    val maintenanceIssues: List<String> = emptyList())

 enum class UnitStatus{
     OCCUPIED_PAID,
     OCCUPIED_OVERDUE,
     MAINTENANCE_ISSUE,
     VACANT
 }

@Serializable
data class Utilities(
    val water: UtilityStatus = UtilityStatus.PAID,
    val electricity: UtilityStatus = UtilityStatus.PAID,
    val trash: UtilityStatus =UtilityStatus.PAID
)

enum class UtilityStatus{
    PAID, DUE
}