package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.E_WateringFirebase
import com.example.weather2.Repository.WateringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WateringViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WateringRepository()

    // StateFlows cho UI
    private val _wateringData = MutableStateFlow<E_WateringFirebase?>(null)
    val wateringData: StateFlow<E_WateringFirebase?> = _wateringData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        observeWateringData()
    }

    /**
     * Lắng nghe thay đổi watering data từ Repository
     */
    fun observeWateringData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getWateringData().collect { data ->
                    _wateringData.value = data
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * Cập nhật toàn bộ watering data
     */
    fun updateWateringData(data: E_WateringFirebase) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateWateringData(data).fold(
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
     * Cập nhật trạng thái watering (0=OFF, 1=ON)
     */
    fun setWateringStatus(status: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.setWateringStatus(status).fold(
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
     * Cập nhật timer watering
     */
    fun updateWateringTimer(timerStart: Int, timerEnd: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateWateringTimer(timerStart, timerEnd).fold(
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
     * Cập nhật humidity land thresholds
     */
    fun updateHumidityLandThresholds(humidityMax: Int, humidityMin: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateHumidityLandThresholds(humidityMax, humidityMin).fold(
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
     * Cập nhật một field cụ thể
     */
    fun updateField(fieldName: String, value: Any) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateField(fieldName, value).fold(
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