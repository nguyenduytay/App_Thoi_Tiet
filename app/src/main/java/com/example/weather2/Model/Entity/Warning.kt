package com.example.weather2.Model.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warning")
data class Warning(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Nhiệt độ (tách status thành Max/Min)
    val tempMax: Int = 0,
    val tempStatusMax: Boolean = false,
    val tempMin: Int = 0,
    val tempStatusMin: Boolean = false,

    // Độ ẩm không khí (tách status)
    val humidityAirMax: Int = 0,
    val humidityAirStatusMax: Boolean = false,
    val humidityAirMin: Int = 0,
    val humidityAirStatusMin: Boolean = false,

    // Độ ẩm đất (tách status)
    val humidityLandMax: Int = 0,
    val humidityLandStatusMax: Boolean = false,
    val humidityLandMin: Int = 0,
    val humidityLandStatusMin: Boolean = false,

    // Thêm trạng thái tổng
    val status: Boolean = false
)