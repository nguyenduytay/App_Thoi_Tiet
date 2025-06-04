package com.example.weather2.Model.Entity

// thông tin thời tiết 7 ngày
data class E_Weather7dFirebase (
    var temp_max : Double = 0.0,
    var temp_min : Double = 0.0,
    var rain : Double = 0.0
) {
    // Constructor không tham số (bắt buộc cho Firebase)
    constructor() : this(0.0, 0.0, 0.0)
}