package com.example.weather2.Model.Entity

data class E_WarningConfigFirebase(
    var fcmToken: String = "chLTIif6QGK0abHUAh9j58:APA91bGDRDjXkniyCxRxDMvAHlKmz9k6xBVpCP7mEpQ3HAgC9tvqTHEJfLRTHmiKDJVUo5LEf1FFUhABpQ75C9cCWAnTyo-g80QjqaEe_paCmKI9zhNU3vI",
    var status : Int =0,
    var tempStatusMax: Int = 1,
    var tempMax: Int = 137,
    var tempStatusMin: Int = 1,
    var tempMin: Int = 124,
    var humidityAirStatusMax: Int = 1,
    var humidityAirMax: Int = 70,
    var humidityAirStatusMin: Int = 1,
    var humidityAirMin: Int = 60,
    var humidityLandStatusMax: Int = 0,
    var humidityLandMax: Int = 80,
    var humidityLandStatusMin: Int = 0,
    var humidityLandMin: Int = 75,
)

