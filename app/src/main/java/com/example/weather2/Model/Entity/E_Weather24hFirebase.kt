package com.example.weather2.Model.Entity

// thông tin thời tiết 24 h
data class E_Weather24hFirebase (
    var temp : Double = 0.0,
    var humidity : Double = 0.0,
    var rain : Double = 0.0
) {
    // Constructor không tham số (bắt buộc cho Firebase)
    constructor() : this(0.0, 0.0,0.0)
}