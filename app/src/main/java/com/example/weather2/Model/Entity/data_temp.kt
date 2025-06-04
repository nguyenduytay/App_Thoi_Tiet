package com.example.weather2.Model.Entity

// thông tin dữ liệu nhiệt độ
data class data_temp(
    val temp: Double = 0.0,
    val time: String = ""
) {
    constructor() : this(0.0, "")
}