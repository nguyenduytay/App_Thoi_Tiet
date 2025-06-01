package com.example.weather2.Model.Entity

data class E_NotificationConfigFirebase(
    var status: Boolean = false,
    var time: Int = 0,
    var temp:Boolean = false,
    var humidityAir: Boolean = false,
    var humidityLand: Boolean = false
)