package com.example.weather2.Model.Entity

data class Weather7dData (
    val time: String,
    val temperatureMax: Int,
    val temperatureMin: Int,
    val rainProbability: Int,
    val icon_morning: Int,
    val icon_evening: Int
)