package dev.csse.pfnassar.daytripper.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import dev.csse.pfnassar.daytripper.Route
import java.time.LocalTime

class DTViewModel : ViewModel() {
    val routeList = mutableStateListOf<Route>()

    init {}

    fun findRouteById(id: String): Route? {
        return routeList.find { it.id.toString() == id }
    }

    fun addRoute(route: Route) {
        routeList.add(route)
    }

    fun deleteRoute(route: Route) {
        routeList.remove(route)
    }

    fun createTestRoutes(count: Int) {
        for (i in 1..count) {
            addRoute(
                Route(
                    location = "Test Location $i",
                    time = LocalTime.now().plusHours(i.toLong())
                )
            )
        }
    }

    val completedRoutesExist: Boolean
        get() = routeList.count { it.completed } > 0

    fun deleteCompletedRoutes() {
        // Remove only routes that are completed
        routeList.removeIf { it.completed }
    }


    fun toggleRouteCompleted(route: Route): Route {
        // Observer of MutableList not notified when changing a property, so
        // need to replace element in the list for notification to go through
        val index = routeList.indexOf(route)
        val newRoute = route
            .copy(completed = !route.completed)
        routeList[index] = newRoute
        return newRoute
    }


    fun setTime(route: Route, time: LocalTime): Route {
        val index = routeList.indexOf(route)
        val newRoute = route
            .copy(time = time)
        routeList[index] = newRoute
        return newRoute
    }

    fun setLocation(route: Route, location: String) {
        val index = routeList.indexOf(route)
        if (index >= 0) {
            routeList[index] = route.copy(location = location)
        }
    }

}