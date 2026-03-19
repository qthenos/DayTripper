package dev.csse.pfnassar.daytripper.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.csse.pfnassar.daytripper.Route
import dev.csse.pfnassar.daytripper.data.RouteRepository
import dev.csse.pfnassar.daytripper.data.db.DayTripperDatabase
import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable
    data object TripList

    @Serializable
    data object Map
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DTApp() {
    val context = LocalContext.current
    val repository = remember(context) {
        val database = DayTripperDatabase.getInstance(context)
        RouteRepository(database.routeDao())
    }
    val factory = remember(repository) { DTViewModelFactory(repository) }
    val model: DTViewModel = viewModel(factory = factory)

    val navController = rememberNavController()

    val backStackEntry by navController
        .currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        bottomBar = {
            if (currentRoute?.endsWith(Routes.TripList.toString()) == true) {
                StyledCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        enabled = model.routeCount > 0 && !model.emptyRouteExists,
                        onClick = { navController.navigate(Routes.Map) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Let's get going!")
                    }
                }
            } else {
                // TODO: Bottom Card for map screen
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TripList
        ) {
            composable<Routes.TripList> {
                TripListScreen(
                    model = model,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )
            }
            composable<Routes.Map> {
                MapScreen(
                    model = model,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    onTripListClick = {
                        navController.navigate(Routes.TripList)
                    }
                )
            }
        }
    }
}

//@Composable
//fun botBar(
//    navController: NavController
//) {
//    val backStackEntry by navController
//        .currentBackStackEntryAsState()
//    val currentRoute = backStackEntry?.destination?.route
//
//
//    NavigationBar {
//        AppScreen.entries.forEach { item ->
//            val selected = currentRoute?.endsWith(item.route.toString()) == true
//            NavigationBarItem(
//                selected = selected,
//                onClick = {
//                    navController.navigate(item.route)
//                },
//                icon = {
//                    Icon(painterResource(item.icon), contentDescription = item.title)
//                },
//                label = {
//                    Text(item.title)
//                })
//        }
//    }
//}
