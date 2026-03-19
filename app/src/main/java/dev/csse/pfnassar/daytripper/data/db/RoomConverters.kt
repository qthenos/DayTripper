package dev.csse.pfnassar.daytripper.data.db

import androidx.room.TypeConverter
import com.mapbox.geojson.Point
import java.time.LocalTime

class RoomConverters {
    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? =
        value?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) }

    @TypeConverter
    fun pointToString(value: Point?): String? = value?.toJson()

    @TypeConverter
    fun stringToPoint(value: String?): Point? =
        value?.takeIf { it.isNotBlank() }?.let { Point.fromJson(it) }
}
