package com.example.weather2.Model.Entity

// dữ liệu hẹn giờ tưới nước và ngưỡng tưới nước
data class E_WateringFirebase(
    var status_timer: Int? = null,
    var timer_start: Int? = null,
    var timer_end: Int? = null,
    var repeat: String? = null,
    var humidity_land_max: Int? = null,
    var humidity_land_min: Int? = null
)
