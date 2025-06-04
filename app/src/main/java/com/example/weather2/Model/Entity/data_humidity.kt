package com.example.weather2.Model.Entity

// thông tin dữ liệu độ ẩm không khí
data class data_humidity(
    val humidity: Double,
    val time: String = ""
) {
    constructor() : this(0.0, "")
}