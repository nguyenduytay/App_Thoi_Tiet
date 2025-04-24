package com.example.weather2.Repository

import com.example.weather2.Model.Dao.Weather24hDao
import com.example.weather2.Model.Entity.Weather24h

class Weather24hRepository(private val weather24hDao: Weather24hDao) {
    suspend fun insertWeather24h(weather24h : Weather24h) {
        return weather24hDao.insertWeather24h(weather24h)
    }
    suspend fun getAllWeather24h() : List<Weather24h>
    {
        return weather24hDao.getAllWeather24h()
    }
    suspend fun updateWeather24h(weather24h : Weather24h)
    {
        return weather24hDao.updateWeather24h(weather24h)
    }
    suspend fun deleteAllWeather24h() {
        weather24hDao.deleteAllWeather24h()
    }

}