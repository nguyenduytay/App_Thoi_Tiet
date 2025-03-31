package com.example.weather2.Model.Dao

import androidx.room.*
import com.example.weather2.Model.Entity.Timer
import com.example.weather2.Model.Entity.Warning
import kotlinx.coroutines.flow.Flow

@Dao
interface WarningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarning(warning: Warning)

    @Query("SELECT * FROM warning")
    suspend fun getAllWarnings(): List<Warning>

    @Query("DELETE FROM warning WHERE id = :id")
    suspend fun deleteWarning(id: Int)

    @Query("SELECT * FROM warning WHERE id = :id")
    fun getWarning(id: Int) : Flow<Warning>

    @Update
    suspend fun updateWarning(warning: Warning)
}
