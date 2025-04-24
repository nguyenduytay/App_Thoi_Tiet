package com.example.weather2.Repository


import com.example.weather2.Model.Dao.Weather7dDao
import com.example.weather2.Model.Entity.Weather7d


class Weather7dRepository(private val weather7dDao: Weather7dDao) {
    suspend fun insertWeather24h(weather7d : Weather7d) {
        return weather7dDao.insertWeather7d(weather7d)
    }
    suspend fun getAllWeather24h() : List<Weather7d>
    {
        return weather7dDao.getAllWeather7d()
    }
    suspend fun updateWeather24h(weather7d : Weather7d)
    {
        return weather7dDao.updateWeather7d(weather7d)
    }
    suspend fun deleteAllWeather7d() {
        weather7dDao.deleteAllWeather7d()
    }
}