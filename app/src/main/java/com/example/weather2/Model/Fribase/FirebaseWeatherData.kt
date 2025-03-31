package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_WeatherDataFirebase
import com.google.firebase.database.*

object FirebaseWeatherData {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("weather_data")

    private var humidity: Double? = null
    private var humidityLand: Double? = null
    private var light: Int? = null
    private var pressure: Double? = null
    private var rain: Int? = null
    private var temperature: Double? = null

    private val listeners = mutableListOf<(E_WeatherDataFirebase) -> Unit>()

    init {
        listenForChanges()
    }
    private fun listenForChanges() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    humidity = snapshot.child("humidity").getValue(Double::class.java)
                    humidityLand = snapshot.child("humidityLand").getValue(Double::class.java)
                    light = snapshot.child("light").getValue(Int::class.java)
                    pressure = snapshot.child("pressure").getValue(Double::class.java)
                    rain = snapshot.child("rain").getValue(Int::class.java)
                    temperature = snapshot.child("temperature").getValue(Double::class.java)
                    val weatherData = E_WeatherDataFirebase(humidity, humidityLand, light, pressure, rain, temperature)
                    notifyListeners(weatherData)
                    Log.d("FirebaseData", "Dữ liệu cập nhật: $weatherData")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Lỗi khi lấy dữ liệu", error.toException())
            }
        })
    }
    fun getWeatherData(): E_WeatherDataFirebase {
        return E_WeatherDataFirebase(humidity, humidityLand, light, pressure, rain, temperature)
    }

    fun addListener(listener: (E_WeatherDataFirebase) -> Unit) {
        listeners.add(listener)
    }

    fun notifyListeners(weatherData: E_WeatherDataFirebase) {
        listeners.forEach { it(weatherData) }
    }
    fun removeListener(listener: (E_WeatherDataFirebase) -> Unit) {
        listeners.remove(listener)
    }
    fun updateWeatherData(weatherData: E_WeatherDataFirebase) {
        database.setValue(weatherData)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Dữ liệu cập nhật thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdateError", "Lỗi khi cập nhật dữ liệu", e)
            }
    }


}
