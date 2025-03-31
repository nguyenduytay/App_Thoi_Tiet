package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.google.firebase.database.*

object FirebaseWarningConfig {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("local_warnings")
    private var warningConfig: E_WarningConfigFirebase? = null
    private val listeners = mutableListOf<(E_WarningConfigFirebase) -> Unit>()
    init {
        listenForChanges()
    }
    private fun listenForChanges() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newConfig = snapshot.getValue(E_WarningConfigFirebase::class.java)
                    if (newConfig != null) {
                        warningConfig = newConfig
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
    fun getWarningConfig(): E_WarningConfigFirebase? {
        return warningConfig
    }
    fun addListener(listener: (E_WarningConfigFirebase) -> Unit) {
        listeners.add(listener)
    }
    private fun notifyListeners(newConfig: E_WarningConfigFirebase) {
        listeners.forEach { it(newConfig) }
    }
    fun removeListener(listener: (E_WarningConfigFirebase) -> Unit) {
        listeners.remove(listener)
    }
    fun updateWarningConfig(newConfig: E_WarningConfigFirebase) {
        database.setValue(newConfig)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Dữ liệu cập nhật thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdateError", "Lỗi khi cập nhật dữ liệu", e)
            }
    }
}
