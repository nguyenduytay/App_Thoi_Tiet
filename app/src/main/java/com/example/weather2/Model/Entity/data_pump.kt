package com.example.weather2.Model.Entity

//thông tin dữ liệu máy bơm
data class data_pump(
    val status: Boolean = false,
    val time: String = ""
) {
    constructor() : this(false, "")
}