package com.example.weather2.Model.Entity

data class E_NotificationConfigFirebase(
    val status: Boolean = false,
    val time: Int = 0,
    val temp:Boolean = false,
    val humidityAir: Boolean = false,
    val humidityLand: Boolean = false
)