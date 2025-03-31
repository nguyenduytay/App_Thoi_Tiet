package com.example.weather2.Repository

import com.example.weather2.Model.Dao.NotificationDao
import com.example.weather2.Model.Entity.Notification
import com.example.weather2.Model.Entity.Timer
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {
    suspend fun getAllNotifications(): List<Notification> {
        return notificationDao.getAllNotifications()
    }
    suspend fun insert(notification: Notification) {
        notificationDao.insertNotification(notification)
    }

    suspend fun delete(id: Int) {
        notificationDao.deleteNotification(id)
    }

    fun getNotification(id : Int) : Flow<Notification>
    {
       return notificationDao.getNotification(id)
    }
    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }
}
