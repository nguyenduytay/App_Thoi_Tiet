// com.example.weather2.Model.Fribase
package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_WindowBlinds
import com.google.firebase.database.*

object FirebaseWindowBlinds {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("window_blinds")
    private var status: Int? = 0
    private var statusAutomatic: Int? = 0

    // Danh sách listeners cho từng trạng thái
    private val statusListeners = mutableListOf<(Int) -> Unit>()
    private val statusAutomaticListeners = mutableListOf<(Int) -> Unit>()

    init {
        listenForChanges()
    }
    // Thêm listener cho trạng thái của màn che
    fun addStatusListener(listener: (Int) -> Unit) {
        statusListeners.add(listener)
        status?.let { listener(it) } // Gọi listener với giá trị hiện tại nếu có
    }
    // Thêm listener cho trạng thái tự động màn che
    fun addStatusAutomaticListener(listener: (Int) -> Unit) {
        statusAutomaticListeners.add(listener)
        statusAutomatic?.let { listener(it) } // Gọi listener với giá trị hiện tại nếu có
    }
    // Thông báo cho các listeners của trạng thái rèm cửa
    private fun notifyStatusListeners(newStatus: Int) {
        statusListeners.forEach { it(newStatus) }
    }
    // Thông báo cho các listeners của trạng thái tự động
    private fun notifyStatusAutomaticListeners(newStatusAutomatic: Int) {
        statusAutomaticListeners.forEach { it(newStatusAutomatic) }
    }
    // Lắng nghe sự thay đổi của dữ liệu trong Firebase
    private fun listenForChanges() {
       //lắng nghe sự thay đổi trạng thái thủ công
        database.child("status").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val newStatus = snapshot.getValue(Int::class.java)
                newStatus?.let{
                    status=it
                    notifyStatusListeners(it)
                    Log.d("FirebaseData","Trạng thái màn che : $it")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.d("FirebaseError","Lỗi khi lấy trạng thái ",error.toException())
            }
        })
        // lắng nghe riêng cho trạng thái tự động
        database.child("status_automatic").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val newStatusAutomatic = snapshot.getValue(Int::class.java)
                newStatusAutomatic?.let{
                    statusAutomatic = it
                    notifyStatusAutomaticListeners(it)
                    Log.d("FirebaseData","Trạng thái tự động che màn : $it")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.d("FirebaseError","Lỗi khi lấy trạng thái tự động ",error.toException())
            }
        })

    }

    // Cập nhật trạng thái của màn che
    fun setWindowBlindsStatus(newStatus: Int) {
        if (newStatus == 0 || newStatus == 1) {
            database.child("status").setValue(newStatus)
                .addOnSuccessListener {
                    Log.d("FirebaseUpdate", "Cập nhật trạng thái rèm cửa: $newStatus")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Lỗi cập nhật", e)
                }
        } else {
            Log.e("FirebaseError", "Giá trị không hợp lệ: $newStatus (chỉ chấp nhận 0 hoặc 1)")
        }
    }

    // Cập nhật trạng thái tự động của rèm cửa
    fun setWindowBlindsStatusAutomatic(newStatus: Int) {
        if (newStatus == 0 || newStatus == 1) {
            database.child("status_automatic").setValue(newStatus)
                .addOnSuccessListener {
                    Log.d("FirebaseUpdate", "Cập nhật trạng thái tự động rèm cửa: $newStatus")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Lỗi cập nhật", e)
                }
        } else {
            Log.e("FirebaseError", "Giá trị không hợp lệ: $newStatus (chỉ chấp nhận 0 hoặc 1)")
        }
    }
}
