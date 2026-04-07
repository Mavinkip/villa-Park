package com.villapark.app.presentation.landlord.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults.contentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.villapark.app.data.models.MaintenanceIssue
import com.villapark.app.data.models.Priority
import com.villapark.app.data.models.RentalUnitStatus
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordDashboard(
    viewModel: LandlordViewModel
){
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit){
        viewModel.loadData()
    }

    Scaffold(
        topBar= {
            TopAppBar(
                title = { Text("Villa Park - Landlord")},
                actions= {
                    IconButton(onClick= {viewModel.onEvent(LandlordEvent.RefreshData(true))}){
                        Text("Reresh")
                    }
                }
            )

        },


        bottomBar = {
            NavigationBar{
                NavigationBarItem(
                    selected = state.selectedTab ==0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Text("🏠", fontSize = 24.sp) },
                    label = {Text("Unit")}
                )
                NavigationBarItem(
                    selected = state.selectedTab == 1,
                    onClick={ viewModel.selectTab(1) },
                    icon = {Text("M")},
                    label = { Text("Maintenace")}
                )
                NavigationBarItem(
                    selected = state.selectedTab == 2,
                    onClick={ viewModel.selectTab(2) },
                    icon = {Text("R")},
                    label = { Text("Report")}
                )
            }

        }
    ){ paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            if(state.isLoading){
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),

                )
            } else{
                when (state.selectedTab){
                    0 -> UnitsTab(
                        units = state.units,
                        stats = state.stats,  // ✅ Fixed // 
                        onUnitClick = {/*Navvigate to unit details*/}
                    )
                    1 -> MaintenanceTab(
                        issue = state.maintenanceIssues,
                        onMarkFixed={ issueId -> viewModel.markIssueAsFixed(issueId)},
                        onCallPlumber = { issueId->
                            viewModel.onEvent(LandlordEvent.CallPlumber(issueId))
                        }
                    )
                    2-> ReportTab(
                        stats = state.stats
                    )
                }
            }
        }

    }

}

@Composable
fun UnitsTab(
    units: List<com.villapark.app.data.models.Unit>,
    stats: DashboardStats,
    onUnitClick: (String) -> Unit
){
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding= PaddingValues(16.dp),
        verticalArrangement =  Arrangement.spacedBy(16.dp)
    ){
        //stas Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Total Units",
                    value = stats.totalUnits.toString(),
                    text = "TU",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Occupied",
                    value = stats.occupiedUnits.toString(),
                    text = "OC",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item{
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                StatCard(
                    title=" Overdue",
                    value = stats.overduePayments.toString(),
                    text="Over",
                    modifier=Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
                StatCard(
                    title = "Issue",
                    value = stats.openIssues.toString(),
                    text = "Open",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Units Grid
        item {
            Text(
                text = "Units Overview",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item{
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                items(units){ unit ->
                UnitGridItem(
                    unit = unit,
                    onClick = { onUnitClick(unit.id)}
                )}
            }
        }


    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    text: String="",
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
){
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha= 0.1f)
        )
    ){
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Column{
                Text(
                    text = title,
                    style =MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color
                )
            }
        }
    }
}
@Composable
fun UnitGridItem(
    unit: com.villapark.app.data.models.Unit,
    onClick: ()-> Unit
){
    val backgroundColor = when (unit.status) {
        UnitStatus.OCCUPIED_PAID -> MaterialTheme.colorScheme.primary
        UnitStatus.OCCUPIED_OVERDUE -> MaterialTheme.colorScheme.error
        UnitStatus.MAINTENANCE_ISSUE -> MaterialTheme.colorScheme.secondary
        UnitStatus.VACANT -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.aspectRatio(1f)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick
    ){
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Text(
                    text = unit.number,
                    style= MaterialTheme.typography.titleLarge,
                    color = when (unit.status){
                        UnitStatus.VACANT -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onPrimary
                    }
                )
                when (unit.status){
                    UnitStatus.OCCUPIED_OVERDUE ->{
                        Text(
                            text = "Overdue",
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    UnitStatus.MAINTENANCE_ISSUE ->{
                        Text(
                            text= "Maintenance",
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    UnitStatus.VACANT ->{
                        Text(
                            text="Vacant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface

                        )
                    }
                    else->{}
                }
            }
        }
    }
}

@Composable
fun MaintenanceTab(
    issue: List<MaintenanceIssue>,
    onMarkFixed: (String) -> Unit,
    onCallPlumber: (String)-> Unit
){
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ){
        items(issue){issue ->
            MaintenanceIssueCard(
                issue = issue,
                onMarkFixed = {onMarkFixed(issue.id)},
                onCallPlumber= {onCallPlumber(issue.id)}
            )
        }
    }
}
@Composable
fun MaintenanceIssueCard(
    issue: MaintenanceIssue,
    onMarkFixed: () -> Unit,
    onCallPlumber: (String) -> Unit
){
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when(issue.priority){
                Priority.URGENT -> MaterialTheme.colorScheme.errorContainer
                Priority.ROUTINE ->  MaterialTheme.colorScheme.secondaryContainer

            }
        )
    ){
        Column(
            modifier = Modifier.padding(16.dp)
        ){
            Row (
                modifier=Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Column{
                    Text(
                        text= "Unit ${issue.unitId}",
                        style= MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = issue.title,
                        style = MaterialTheme.typography.bodyLarge

                    )
                    Text(
                        text= issue.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines =2
                    )
                    Text(
                        text="Reported by: ${issue.tenantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Card (
                colors = CardDefaults.cardColors(
                    containerColor =when (issue.priority){
                        Priority.URGENT -> MaterialTheme.colorScheme.errorContainer
                        Priority.ROUTINE -> MaterialTheme.colorScheme.secondary
                    }
                )
            ){
                Text(
                    text = issue.priority.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ){
            TextButton(
                onClick = onCallPlumber as () -> Unit,
                colors = ButtonDefaults.textButtonColors(
                   contentColor = MaterialTheme.colorScheme.secondary
                )
            ){
                Text("Call Plumber")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onMarkFixed,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )

            ){
                Text("Mark Fixed")
            }
        }

    }
}

@Composable
fun ReportTab(
    stats: DashboardStats
){
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ){
        item{
            Card(
                modifier =Modifier.fillMaxWidth()
            ){
                Column(
                    modifier = Modifier.padding(16.dp)
                ){
                    Text(
                        text= "Monthly Revenue",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text ="Ksh ${stats.monthlyRevenue}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item{
            Card(
                modifier = Modifier.fillMaxWidth()
            ){
                Column(
                    modifier = Modifier.padding(16.dp)

                ){
                    Text(
                        text = "Occupancy rate",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${(stats.occupiedUnits * 100 / max(stats.totalUnits, 1))}%",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${stats.occupiedUnits} out of ${stats.totalUnits} units occupied",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Button(
                onClick = {/*generate pdf report*/},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Generate Monthly Report ")
            }
        }
    }
}


@Composable
fun UnitCard(
    unit: com.villapark.app.data.models.Unit
){
    val backgroundColor = when (unit.status){
        UnitStatus.OCCUPIED_PAID -> MaterialTheme.colorScheme.primary
        UnitStatus.OCCUPIED_OVERDUE-> MaterialTheme.colorScheme.error
        UnitStatus.MAINTENANCE_ISSUE -> MaterialTheme.colorScheme.secondary
        UnitStatus.VACANT-> MaterialTheme.colorScheme.surfaceVariant
        else -> {}
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor as Color
        )
    ){
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            )   {
                Text(
                    text = unit.number,
                    style = typography.titleLarge,
                    color= MaterialTheme.colorScheme.onPrimary

                )
                if (unit.status == UnitStatus.OCCUPIED_OVERDUE){
                    Text(
                        text = "⚠️",
                        fontSize = 16.sp,
                        color = contentColor
                    )
                }
            }         }
    }
}





@Composable
fun MaintenanceItem(
    issue: MaintenanceIssue,
    onMarkFixed: () -> Unit
){
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when(issue.priority){
                Priority.URGENT -> MaterialTheme.colorScheme.errorContainer
                Priority.ROUTINE -> MaterialTheme.colorScheme.secondaryContainer
            }
        )

    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Column{
                Text(
                    text ="Unit ${issue.unitId}",
                    style = typography.titleMedium
                )
                Text(
                    text= issue.title,
                    style = typography.bodyMedium,

                )
                Text(
                    text = "Priority: ${issue.priority}",
                    style = typography.labelSmall,
                    color = when (issue.priority){
                        Priority.URGENT -> MaterialTheme.colorScheme.error
                        Priority.ROUTINE -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
            // Quick action
            Row{
                IconButton(
                    onClick= onMarkFixed
                ){
                    
                }
            }
        }
    }
}