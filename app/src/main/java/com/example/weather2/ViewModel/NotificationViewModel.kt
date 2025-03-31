package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.*
import com.example.weather2.Database.AppDatabase
import com.example.weather2.Model.Entity.Notification
import com.example.weather2.Model.Entity.Warning
import com.example.weather2.Repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NotificationRepository

    // LiveData để quan sát danh sách thông báo
    private val _allNotifications = MutableLiveData<List<Notification>>()
    val allNotifications: LiveData<List<Notification>> get() = _allNotifications

    init {
        val notificationDao = AppDatabase.getDatabase(application).notificationDao()
        repository = NotificationRepository(notificationDao)
        loadAllNotifications()
    }
    // Hàm load dữ liệu từ repository
    private fun loadAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            _allNotifications.postValue(repository.getAllNotifications())
        }
    }
    fun insert(notification: Notification) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(notification)
            loadAllNotifications()
        }
    }
    fun delete(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(id)
            loadAllNotifications()
        }
    }
    fun updateNotification(notification : Notification) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNotification(notification)
        }
    }
    fun getNotification(id: Int): Flow<Notification> {
        return repository.getNotification(id)
    }
}
