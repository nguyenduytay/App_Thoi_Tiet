package com.example.weather2.Model.Dao

import androidx.room.*
import com.example.weather2.Model.Entity.Notification
import com.example.weather2.Model.Entity.Timer
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("SELECT * FROM notification")
    suspend fun getAllNotifications(): List<Notification>

    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    @Query("SELECT * FROM notification WHERE id = :id")
    fun getNotification(id: Int) : Flow<Notification>

    @Update
    suspend fun updateNotification(notification: Notification)
}
