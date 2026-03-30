package com.villapark.app.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MaintenanceIssue(
    val id: String= "",
    val unitId: String="",
    val tenantId: String= "",
    val tenantName:String="",
    val title:String="",
    val description:String="",
    val priority: Priority =Priority.ROUTINE,
    val status: IssueStatus = IssueStatus.OPEN,
    val imageUrl:String?=null,
    val createdAt: LocalDateTime?=null,
    val resolvedAt: LocalDateTime?=null,


)
enum class Priority{
    URGENT, ROUTINE
}
 enum class IssueStatus{
     OPEN, IN_PROGRESS, RESOLVED
 }

