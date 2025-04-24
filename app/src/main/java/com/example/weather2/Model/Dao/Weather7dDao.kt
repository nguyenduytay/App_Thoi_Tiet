package com.example.weather2.Model.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weather2.Model.Entity.Weather7d

@Dao
interface Weather7dDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather7d(weather7d: Weather7d)

    @Query("SELECT * FROM weather7d")
    suspend fun getAllWeather7d() : List<Weather7d>

    @Update
    suspend fun updateWeather7d(weather7d : Weather7d)

    @Query("DELETE FROM weather7d")
    suspend fun deleteAllWeather7d()

}