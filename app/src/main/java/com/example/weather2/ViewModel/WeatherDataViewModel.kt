package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.E_WeatherDataFirebase
import com.example.weather2.Repository.WeatherDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherDataViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherDataRepository()

    private val _currentWeatherData = MutableStateFlow<E_WeatherDataFirebase?>(null)
    val currentWeatherData: StateFlow<E_WeatherDataFirebase?> = _currentWeatherData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        observeCurrentWeatherData()
    }

    fun observeCurrentWeatherData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getCurrentWeatherData().collect { weatherData ->
                    _currentWeatherData.value = weatherData
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun updateWeatherData(weatherData: E_WeatherDataFirebase) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateWeatherData(weatherData).fold(
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

    fun updatePumpStatus(isOn: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updatePumpStatus(isOn).fold(
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

    fun updateBlindStatus(isOn: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateBlindStatus(isOn).fold(
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