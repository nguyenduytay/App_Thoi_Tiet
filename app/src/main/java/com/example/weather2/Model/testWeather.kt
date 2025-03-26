package com.example.weather2.Model

data class testWeather(
    var id: String ?= "",
    var temperature: Int? = 0,
    var humidity: Int? = 0,
    var temperatureMax:Int?=0,
    var temperatureMin:Int?=0
)