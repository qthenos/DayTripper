package dev.csse.pfnassar.daytripper

import com.mapbox.geojson.Point
import java.time.LocalTime


var lastRouteId = 0

data class Route(
    val id: Int = lastRouteId++,
    val location: String = "",
    val coordinate: Point? = null, // Mapbox GeoJSON Point — compatible with markers, routes, navigation
    val time: LocalTime? = null,
    val completed: Boolean = false
)