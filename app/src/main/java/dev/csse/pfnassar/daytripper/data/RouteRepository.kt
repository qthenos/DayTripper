package dev.csse.pfnassar.daytripper.data

import com.mapbox.geojson.Point
import dev.csse.pfnassar.daytripper.Route
import dev.csse.pfnassar.daytripper.data.db.RouteDao
import dev.csse.pfnassar.daytripper.data.db.RouteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

class RouteRepository(
    private val routeDao: RouteDao
) {
    val routes: Flow<List<Route>> = routeDao.observeAllRoutes().map { rows ->
        rows.map { it.toDomain() }
    }

    suspend fun findById(id: Int): Route? = routeDao.findById(id)?.toDomain()

    suspend fun addRoute(route: Route) {
        routeDao.insert(route.toNewEntity())
    }

    suspend fun updateRoute(route: Route) {
        routeDao.update(route.toEntity())
    }

    suspend fun deleteRoute(route: Route) {
        routeDao.delete(route.toEntity())
    }

    suspend fun deleteCompletedRoutes() {
        routeDao.deleteCompleted()
    }

    suspend fun resetTrip() {
        routeDao.deleteAll()
        routeDao.resetSequence()
    }

    private fun RouteEntity.toDomain(): Route {
        return Route(
            id = id,
            location = location,
            coordinate = coordinateJson?.let { Point.fromJson(it) },
            time = timeIso?.let { LocalTime.parse(it) },
            completed = completed
        )
    }

    private fun Route.toEntity(): RouteEntity {
        return RouteEntity(
            id = id,
            location = location,
            coordinateJson = coordinate?.toJson(),
            timeIso = time?.toString(),
            completed = completed
        )
    }

    private fun Route.toNewEntity(): RouteEntity {
        return RouteEntity(
            id = 0,
            location = location,
            coordinateJson = coordinate?.toJson(),
            timeIso = time?.toString(),
            completed = completed
        )
    }
}
