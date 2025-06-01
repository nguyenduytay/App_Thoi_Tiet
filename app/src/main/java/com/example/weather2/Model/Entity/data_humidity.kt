package com.example.weather2.Model.Entity

data class data_humidity(
    val humidity: Double,
    val time: String = ""
) {
    constructor() : this(0.0, "")
}