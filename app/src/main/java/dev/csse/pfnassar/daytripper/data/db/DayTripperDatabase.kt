package dev.csse.pfnassar.daytripper.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RouteEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class DayTripperDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: DayTripperDatabase? = null

        fun getInstance(context: Context): DayTripperDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DayTripperDatabase::class.java,
                    "daytripper.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
