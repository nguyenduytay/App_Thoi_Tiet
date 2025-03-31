package com.example.weather2.Model.Entity

data class E_WarningConfigFirebase(
    val fcmToken: String = "",
    val tempStatusMax: Int = 1,
    val tempMax: Int = 137,
    val tempStatusMin: Int = 1,
    val tempMin: Int = 124,
    val humidityAirStatusMax: Int = 1,
    val humidityAirMax: Int = 70,
    val humidityAirStatusMin: Int = 1,
    val humidityAirMin: Int = 60,
    val humidityLandStatusMax: Int = 0,
    val humidityLandMax: Int = 80,
    val humidityLandStatusMin: Int = 0,
    val humidityLandMin: Int = 75,
)

