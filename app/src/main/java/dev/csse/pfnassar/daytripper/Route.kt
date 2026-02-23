package dev.csse.pfnassar.daytripper

import java.time.LocalTime


var lastRouteId = 0

data class Route(
    val id: Int = lastRouteId++,
    val location: String = "",
    val time: LocalTime? = null,
    val completed: Boolean = false
)