package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_Weather24hFirebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseWeather24h {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("weather_24h")

    // Map lưu danh sách dữ liệu thời tiết theo thời gian
    private var weatherDataMap: MutableMap<String, E_Weather24hFirebase> = mutableMapOf()

    private val listeners = mutableListOf<(Map<String, E_Weather24hFirebase>) -> Unit>()

    init {
        listenForChanges()
    }

    private fun listenForChanges() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newDataMap = mutableMapOf<String, E_Weather24hFirebase>()

                for (timeSnapshot in snapshot.children) {
                    val time = timeSnapshot.key // Lấy thời gian (VD: "2025-04-03 13:00:00")
                    val data = timeSnapshot.getValue(E_Weather24hFirebase::class.java)

                    if (time != null && data != null) {
                        newDataMap[time] = data
                    }
                }

                weatherDataMap = newDataMap
                notifyListeners(newDataMap)
                Log.d("FirebaseData", "Dữ liệu cập nhật 24 h: $newDataMap")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Lỗi khi lấy dữ liệu", error.toException())
            }
        })
    }

    // ✅ Lấy danh sách dữ liệu thời tiết
    fun getWeatherData(): Map<String, E_Weather24hFirebase> {
        return weatherDataMap
    }

    // ✅ Thêm listener để lắng nghe thay đổi
    fun addListener(listener: (Map<String, E_Weather24hFirebase>) -> Unit) {
        listeners.add(listener)
    }

    // ✅ Xóa listener khi không cần thiết
    fun removeListener(listener: (Map<String, E_Weather24hFirebase>) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(newDataMap: Map<String, E_Weather24hFirebase>) {
        listeners.forEach { it(newDataMap) }
    }

    // ✅ Cập nhật toàn bộ dữ liệu trên Firebase
    fun updateWeatherData(newDataMap: Map<String, E_Weather24hFirebase>) {
        database.setValue(newDataMap)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Dữ liệu cập nhật thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdateError", "Lỗi khi cập nhật dữ liệu", e)
            }
    }
}
