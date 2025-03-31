package com.example.weather2.Model.Fribase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseWatering {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("watering")

    private var status: Int? = null
    private val listeners = mutableListOf<(Int) -> Unit>()

    init {
        listenForChanges() // Bắt đầu lắng nghe Firebase khi ứng dụng khởi động
    }

    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    private fun notifyListeners(newStatus: Int) {
        listeners.forEach { it(newStatus) }
    }

    private fun listenForChanges() {
        database.child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                status = snapshot.getValue(Int::class.java) ?: 0
                notifyListeners(status!!)
                Log.d("FirebaseData", "Trạng thái tưới nước: $status")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Lỗi khi lấy dữ liệu", error.toException())
            }
        })
    }

    fun setWateringStatus(newStatus: Int) {
        if (newStatus == 0 || newStatus == 1) { // Kiểm tra giá trị hợp lệ
            database.child("status").setValue(newStatus)
                .addOnSuccessListener {
                    Log.d("FirebaseUpdate", "Cập nhật trạng thái tưới nước: $newStatus")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Lỗi cập nhật", e)
                }
        } else {
            Log.e("FirebaseError", "Giá trị không hợp lệ: $newStatus (chỉ chấp nhận 0 hoặc 1)")
        }
    }
}

