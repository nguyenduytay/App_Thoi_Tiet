package com.example.weather2.Model.Entity

data class data_pump(
    val status: Boolean = false,
    val time: String = ""
) {
    constructor() : this(false, "")
}