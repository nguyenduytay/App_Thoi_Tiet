package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.E_NotificationConfigFirebase
import com.example.weather2.Repository.NotificationConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationConfigRepository()

    // StateFlows cho UI
    private val _notificationConfig = MutableStateFlow<E_NotificationConfigFirebase?>(null)
    val notificationConfig: StateFlow<E_NotificationConfigFirebase?> = _notificationConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        observeNotificationConfig()
    }

    /**
     * Lắng nghe thay đổi notification config từ Repository
     */
    fun observeNotificationConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getNotificationConfig().collect { config ->
                    _notificationConfig.value = config
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * Cập nhật toàn bộ notification config
     */
    fun updateNotificationConfig(config: E_NotificationConfigFirebase) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateNotificationConfig(config).fold(
                onSuccess = {
                    _updateSuccess.value = true
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Cập nhật trạng thái notification
     */
    fun updateNotificationStatus(status: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateNotificationStatus(status).fold(
                onSuccess = {
                    _updateSuccess.value = true
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Cập nhật thời gian notification
     */
    fun updateNotificationTime(time: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateNotificationTime(time).fold(
                onSuccess = {
                    _updateSuccess.value = true
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearUpdateSuccess() {
        _updateSuccess.value = false
    }
}