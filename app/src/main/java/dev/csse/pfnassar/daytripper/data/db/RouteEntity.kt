package dev.csse.pfnassar.daytripper.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val location: String = "",
    val coordinateJson: String? = null,
    val timeIso: String? = null,
    val completed: Boolean = false
)
