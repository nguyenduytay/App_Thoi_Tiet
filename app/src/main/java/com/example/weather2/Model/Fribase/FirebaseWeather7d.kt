package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.example.weather2.Model.Entity.E_Weather7dFirebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseWeather7d {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("weather_7d")
    private var weather7d: E_Weather7dFirebase? = null
    private val listeners = mutableListOf<(E_Weather7dFirebase) -> Unit>()
    init {
        listenForChanges()
    }
    private fun listenForChanges() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newConfig = snapshot.getValue(E_Weather7dFirebase::class.java)
                    if (newConfig != null) {
                        weather7d = newConfig
                        notifyListeners(newConfig)
                        Log.d("FirebaseData", "Dữ liệu cập nhật: $newConfig")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Lỗi khi lấy dữ liệu", error.toException())
            }
        })
    }
    fun getWarningConfig(): E_Weather7dFirebase? {
        return weather7d
    }
    fun addListener(listener: (E_Weather7dFirebase) -> Unit) {
        listeners.add(listener)
    }
    private fun notifyListeners(newConfig: E_Weather7dFirebase) {
        listeners.forEach { it(newConfig) }
    }
    fun removeListener(listener: (E_Weather7dFirebase) -> Unit) {
        listeners.remove(listener)
    }
    fun updateWarningConfig(newConfig: E_Weather7dFirebase) {
        database.setValue(newConfig)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Dữ liệu cập nhật thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdateError", "Lỗi khi cập nhật dữ liệu", e)
            }
    }
}