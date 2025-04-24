package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_NotificationConfigFirebase
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseNotificationConfig {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("local_notification")
    private var notificationConfig: E_NotificationConfigFirebase? = null
    private val listeners = mutableListOf<(E_NotificationConfigFirebase) -> Unit>()
    init {
        listenForChanges()
    }
    private fun listenForChanges() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newConfig = snapshot.getValue(E_NotificationConfigFirebase::class.java)
                    if (newConfig != null) {
                        notificationConfig = newConfig
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
    fun getNotificationConfig(): E_NotificationConfigFirebase? {
        return notificationConfig
    }
    fun addListener(listener: (E_NotificationConfigFirebase) -> Unit) {
        listeners.add(listener)
    }
    private fun notifyListeners(newConfig: E_NotificationConfigFirebase) {
        listeners.forEach { it(newConfig) }
    }
    fun removeListener(listener: (E_NotificationConfigFirebase) -> Unit) {
        listeners.remove(listener)
    }
    fun updateNotificationConfig(newConfig: E_NotificationConfigFirebase) {
        database.setValue(newConfig)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Dữ liệu cập nhật thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdateError", "Lỗi khi cập nhật dữ liệu", e)
            }
    }
}