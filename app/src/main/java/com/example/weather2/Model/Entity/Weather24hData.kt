package com.example.weather2.Model.Entity

data class Weather24hData(
    val time: String,
    val temperature: Int,
    val rainProbability: Int,
    val icon: Int
)