package com.villapark.app.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable



@Serializable
data class Tenant(
    val id: String= "",
    val name: String = "",
    val phoneNumber: String ="",
    val email: String ="",
    val unitId: String ="",
    val currentBalance: Double = 0.0,
    val lastPaymentDate: LocalDateTime? = null,
    val profileImageUrl: String =""

)



