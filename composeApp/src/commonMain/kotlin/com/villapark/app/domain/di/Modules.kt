package com.villapark.app.domain.di

import com.villapark.app.data.repository.FirebaseRentRepository
import com.villapark.app.data.repository.RentRepository
import com.villapark.app.presentation.landlord.dashboard.LandlordViewModel
import com.villapark.app.presentation.tenant.home.TenantHomeViewModel
import com.villapark.app.presentation.tenant.payments.PaymentViewModel
import com.villapark.app.presentation.tenant.issues.IssueReportViewModel
import org.koin.dsl.module

val dataModule = module {
    single<RentRepository> { FirebaseRentRepository() }
}

val viewModelModule = module {
    factory { TenantHomeViewModel(get()) }
    factory { LandlordViewModel(get()) }
    factory { PaymentViewModel(get()) }
    factory { IssueReportViewModel(get()) }
}

val appModules = listOf(dataModule, viewModelModule)
