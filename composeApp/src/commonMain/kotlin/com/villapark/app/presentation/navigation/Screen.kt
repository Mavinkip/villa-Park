package com.villapark.app.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object TenantHome: Screen("tenant/home/{tenantId}"){
        fun passTenantId(tenantId: String) ="tenant/home/$tenantId"
    }
    object LandlordDashboard : Screen("landlord/dashboard")
    object IssueReport : Screen("issue/report/{tenantId}"){
        fun passTenantId(tenantId: String)="issue/report/$tenantId"
    }
    object PaymentHistory: Screen("payment/{tenantId}"){
        fun passTenantId(tenatId: String) = "Payments/$tenatId"
    }
}