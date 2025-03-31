package com.example.weather2.Model.Dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.weather2.Model.Entity.Timer
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: Timer)

    @Query("SELECT * FROM timer ORDER BY id ASC")
    fun getAllTimers(): LiveData<List<Timer>>

    @Query("DELETE FROM timer WHERE id = :id")
    suspend fun deleteTimer(id: Int)

    @Query("SELECT * FROM timer WHERE id = :id")
    fun getTimer(id: Int) : Flow<Timer>

    @Query("UPDATE timer SET status = :status WHERE id = :id")
    suspend fun updateStatus(id : Int, status : Boolean)

    @Update
    suspend fun updateTimer(timer: Timer)

    @Query("SELECT * FROM timer ORDER BY id ASC LIMIT 1")
    fun getFirstTimer(): Flow<Timer?>

}
