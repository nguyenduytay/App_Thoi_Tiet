package com.example.weather2.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weather2.Database.AppDatabase
import com.example.weather2.Model.Entity.Weather7d
import com.example.weather2.Repository.Weather7dRepository
import kotlinx.coroutines.launch

class Weather7dViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: Weather7dRepository
    private val _allWeather7d = MutableLiveData<List<Weather7d>>()

    init {
        val weather7dDao = AppDatabase.getDatabase(application).weather7dDao()
        repository = Weather7dRepository(weather7dDao)
        fetchAllWeather7d()
    }

    private fun fetchAllWeather7d() {
        viewModelScope.launch {
            val weather7ds = repository.getAllWeather24h()
            _allWeather7d.postValue(weather7ds)
        }
    }

    fun insert(weather7d: Weather7d) {
        viewModelScope.launch {
            repository.insertWeather24h(weather7d)
            fetchAllWeather7d()
        }
    }

    fun updateWeather(weather7d: Weather7d) {
        viewModelScope.launch {
            repository.updateWeather24h(weather7d)
            fetchAllWeather7d()
        }
    }

    suspend fun getAllWeather7d(): List<Weather7d> {
        return repository.getAllWeather24h()
    }

    fun refreshWeather7dData(newWeatherList: List<Weather7d>) {
        viewModelScope.launch {
            repository.deleteAllWeather7d()
            newWeatherList.forEach { repository.insertWeather24h(it) }
            fetchAllWeather7d()
        }
    }
}