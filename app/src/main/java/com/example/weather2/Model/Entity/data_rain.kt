package com.example.weather2.Model.Entity

data class data_rain(
    val rain: Int = 0,
    val time: String = ""
) {
    constructor() : this(0, "")
}