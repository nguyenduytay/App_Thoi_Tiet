package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weather2.Database.AppDatabase
import com.example.weather2.Model.Entity.Weather24h
import com.example.weather2.Repository.Weather24hRepository
import kotlinx.coroutines.launch

class Weather24hViewModel(application : Application) : AndroidViewModel(application) {
    private val repository : Weather24hRepository
    private val _allWeather24h = MutableLiveData<List<Weather24h>>()

    init {
        val weather24hDao = AppDatabase.getDatabase(application).weather24hDao()
        repository = Weather24hRepository(weather24hDao)
        fetchAllWeather24h()
    }
    private fun fetchAllWeather24h() {
        viewModelScope.launch {
            val weather24hs = repository.getAllWeather24h()
            _allWeather24h.postValue(weather24hs)
        }
    }
    fun insert(weather24h: Weather24h) {
        viewModelScope.launch {
            repository.insertWeather24h(weather24h)
            fetchAllWeather24h()
        }
    }
    fun updateWeather(weather24h : Weather24h) {
        viewModelScope.launch {
            repository.updateWeather24h(weather24h)
            fetchAllWeather24h()
        }
    }
    suspend fun getAllWeather24h(): List<Weather24h> {
            return repository.getAllWeather24h()
    }
    fun refreshWeather24hData(newWeatherList: List<Weather24h>) {
        viewModelScope.launch {
            repository.deleteAllWeather24h()
            newWeatherList.forEach { repository.insertWeather24h(it) }
            fetchAllWeather24h()
        }
    }

}