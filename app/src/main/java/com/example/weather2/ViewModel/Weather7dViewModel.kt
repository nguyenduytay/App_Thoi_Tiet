package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.Model.Entity.E_Weather7dFirebase
import com.example.weather2.Repository.Weather7dRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Weather7dViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Weather7dRepository()

    private val _weather7dData = MutableStateFlow<Map<String, E_Weather7dFirebase>>(emptyMap())
    val weather7dData: StateFlow<Map<String, E_Weather7dFirebase>> = _weather7dData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        observe7DayWeatherData()
    }

    fun observe7DayWeatherData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.get7DayWeatherData().collect { weatherData ->
                    _weather7dData.value = weatherData
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun update7DayWeatherData(weatherMap: Map<String, E_Weather7dFirebase>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.update7DayWeatherData(weatherMap).fold(
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