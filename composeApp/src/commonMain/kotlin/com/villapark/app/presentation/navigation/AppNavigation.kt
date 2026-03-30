package com.villapark.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.villapark.app.data.repository.FirebaseRentRepository
import com.villapark.app.presentation.auth.LoginScreen
import com.villapark.app.presentation.landlord.dashboard.LandlordDashboard
import com.villapark.app.presentation.landlord.dashboard.LandlordViewModel
import com.villapark.app.presentation.tenant.home.TenantHomeScreen
import com.villapark.app.presentation.tenant.home.TenantHomeViewModel
import com.villapark.app.presentation.tenant.issues.IssueReportScreen
import com.villapark.app.presentation.tenant.payments.PaymentHistoryScreen
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.functions.FirebaseFunctions
import org.koin.dsl.koinApplication
import org.koin.dsl.module


val appModule = module {
    // firebase istaances
    single{dev.gitlive.firebase.firestore.FirebaseFirestore.getInstance()}
    single{dev.gitlive.firebase.functions.FirebaseFunctions.getInstance()}

    // Repositories
    single{com.villapark.app.data.repository.FirebaseRentRepository(get(),get())}
    //viewmodels
    factory{com.villapark.app.presentation.tenant.home.TenantHomeViewModel(get())}
    factory{com.villapark.app.presentation.landlord.dashboard.LandlordViewModel(get())}

}




@Composable
fun AppNavigation(){
    // Initialize koin context
   koinApplication{
       modules(appModule)
   }

    //creat navigator with LoginScreen
    Navigator(screen = LoginScreen(){
        navigator->
        SlideTransition(navigator)
    })
}

