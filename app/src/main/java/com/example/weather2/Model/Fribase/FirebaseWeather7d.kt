package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_Weather7dFirebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseWeather7d {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("weather_7d")

    // Map lưu danh sách dữ liệu thời tiết theo ngày
    private var weatherDataMap: MutableMap<String, E_Weather7dFirebase> = mutableMapOf()

    private val listeners = mutableListOf<(Map<String, E_Weather7dFirebase>) -> Unit>()

    init {
        listenForChanges()
    }

    private fun listenForChanges() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newDataMap = mutableMapOf<String, E_Weather7dFirebase>()

                for (daySnapshot in snapshot.children) {
                    val day = daySnapshot.key
                    val data = daySnapshot.getValue(E_Weather7dFirebase::class.java)

                    if (day != null && data != null) {
                        newDataMap[day] = data
                    }
                }
                weatherDataMap = newDataMap
                notifyListeners(newDataMap)
                Log.d("FirebaseData7d", "Dữ liệu 7 ngày cập nhật: $newDataMap")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Lỗi khi lấy dữ liệu 7 ngày", error.toException())
            }
        })
    }

    // ✅ Lấy danh sách dữ liệu thời tiết 7 ngày
    fun getWeatherData(): Map<String, E_Weather7dFirebase> {
        return weatherDataMap
    }

    // ✅ Thêm listener để lắng nghe thay đổi
    fun addListener(listener: (Map<String, E_Weather7dFirebase>) -> Unit) {
        listeners.add(listener)
    }

    // ✅ Xóa listener khi không cần thiết
    fun removeListener(listener: (Map<String, E_Weather7dFirebase>) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(newDataMap: Map<String, E_Weather7dFirebase>) {
        listeners.forEach { it(newDataMap) }
    }

    // ✅ Cập nhật toàn bộ dữ liệu trên Firebase
    fun updateWeatherData(newDataMap: Map<String, E_Weather7dFirebase>) {
        database.setValue(newDataMap)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Dữ liệu 7 ngày cập nhật thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdateError", "Lỗi khi cập nhật dữ liệu 7 ngày", e)
            }
    }
}