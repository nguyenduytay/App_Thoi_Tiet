package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.example.weather2.Repository.WarningConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WarningConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WarningConfigRepository()

    // StateFlows cho UI
    private val _warningConfig = MutableStateFlow<E_WarningConfigFirebase?>(null)
    val warningConfig: StateFlow<E_WarningConfigFirebase?> = _warningConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        observeWarningConfig()
    }

    /**
     * Lắng nghe thay đổi warning config từ Repository
     */
    fun observeWarningConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getWarningConfig().collect { config ->
                    _warningConfig.value = config
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * Cập nhật toàn bộ warning config
     */
    fun updateWarningConfig(config: E_WarningConfigFirebase) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateWarningConfig(config).fold(
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
     * Cập nhật trạng thái warning tổng thể
     */
    fun updateWarningStatus(status: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateWarningStatus(status).fold(
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
     * Cập nhật temperature thresholds
     */
    fun updateTempThresholds(tempMax: Int, tempMin: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateTempThresholds(tempMax, tempMin).fold(
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
     * Cập nhật humidity air thresholds
     */
    fun updateHumidityAirThresholds(humidityMax: Int, humidityMin: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateHumidityAirThresholds(humidityMax, humidityMin).fold(
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