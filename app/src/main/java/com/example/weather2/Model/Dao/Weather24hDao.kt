package com.example.weather2.Model.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weather2.Model.Entity.Weather24h

@Dao
interface Weather24hDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather24h(weather24h: Weather24h)

    @Query("SELECT * FROM weather24h")
    suspend fun getAllWeather24h() : List<Weather24h>

    @Update
    suspend fun updateWeather24h(weather24h : Weather24h)

    @Query("DELETE FROM weather24h")
    suspend fun deleteAllWeather24h()

}