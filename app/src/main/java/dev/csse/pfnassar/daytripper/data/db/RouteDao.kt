package dev.csse.pfnassar.daytripper.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY id")
    fun observeAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)

    @Delete
    suspend fun delete(route: RouteEntity)

    @Query("DELETE FROM routes WHERE completed = 1")
    suspend fun deleteCompleted()

    @Query("DELETE FROM routes")
    suspend fun deleteAll()

    @Query("DELETE FROM sqlite_sequence WHERE name='routes'")
    suspend fun resetSequence()
}
