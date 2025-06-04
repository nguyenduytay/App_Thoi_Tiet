package com.example.weather2.Model.Entity

// dữ liệu hiển thị thời tiết
data class E_WeatherDataFirebase(
    var humidity: Double = 0.0,
    var humidityLand: Double = 0.0,
    var last_update: String = "",
    var light: Int = 0,
    var pressure: Double = 0.0,
    var rain: Int = 0,
    var status_blind: Boolean = false,
    var status_pump: Boolean = false,
    var auto_mode: Boolean = false,
    var temperature: Double = 0.0
)
