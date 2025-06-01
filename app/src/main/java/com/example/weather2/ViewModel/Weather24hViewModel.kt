package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.E_Weather24hFirebase
import com.example.weather2.Repository.Weather24hRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Weather24hViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Weather24hRepository()

    private val _weather24hData = MutableStateFlow<Map<String, E_Weather24hFirebase>>(emptyMap())
    val weather24hData: StateFlow<Map<String, E_Weather24hFirebase>> = _weather24hData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        observe24HourWeatherData()
    }

    fun observe24HourWeatherData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.get24HourWeatherData().collect { weatherData ->
                    _weather24hData.value = weatherData
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun update24HourWeatherData(weatherMap: Map<String, E_Weather24hFirebase>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.update24HourWeatherData(weatherMap).fold(
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