package com.example.weather2.Model.Entity

data class data_humidity_land(
    val humidity_land: Int = 0,
    val time: String = ""
) {
    constructor() : this(0, "")
}