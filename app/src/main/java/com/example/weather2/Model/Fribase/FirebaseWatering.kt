package com.example.weather2.Model.Fribase

import android.util.Log
import com.example.weather2.Model.Entity.E_WateringFirebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseWatering {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("watering")

    private var status: Int? = null
    private var statusHumidityLand: Int? = null
    private var statusTimer: Int? = null
    private var timerStart: Int? = null
    private var timerEnd: Int? = null
    private var repeat: String? = ""

    private val statusListeners = mutableListOf<(Int) -> Unit>()
    private val statusHumidityLandListeners = mutableListOf<(Int) -> Unit>()
    private val statusTimerListeners = mutableListOf<(Int) -> Unit>()
    private val timerStartListeners = mutableListOf<(Int) -> Unit>()
    private val timerEndListeners = mutableListOf<(Int) -> Unit>()
    private val repeatListeners = mutableListOf<(String) -> Unit>()
    init {
        listenForChanges() // Bắt đầu lắng nghe Firebase khi ứng dụng khởi động
    }

    //thêm listener cho trạng thái tưới nước
    fun addStatusListener(listener: (Int) -> Unit) {
        statusListeners.add(listener)
        status?.let { listener(it) }
    }
    //thêm listener cho trạng thái tưới nước tự đông
    fun addStatusHumidityLandListener(listener: (Int) -> Unit) {
        statusHumidityLandListeners.add(listener)
        statusHumidityLand?.let { listener(it) }
    }
    //thêm listener cho trạng thái tưới nước
    fun addStatusTimerListener(listener: (Int) -> Unit) {
        statusTimerListeners.add(listener)
        statusTimer?.let { listener(it) }
    }
    fun addTimerStartListener(listener: (Int) -> Unit) {
        timerStartListeners.add(listener)
        timerStart?.let { listener(it) }
    }
    fun addTimerEndListener(listener: (Int) -> Unit) {
        timerEndListeners.add(listener)
        timerEnd?.let { listener(it) }
    }
    fun addRepeatListener(listener: (String) -> Unit) {
        repeatListeners.add(listener)
        repeat?.let { listener(it) }
    }
    //thông báo cho các listener
    private fun notifyStatusListeners(newStatus: Int) {
        statusListeners.forEach { it(newStatus) }
    }
    private fun notifyStatusHumidityLandListeners(newStatus: Int) {
        statusHumidityLandListeners.forEach { it(newStatus) }
    }
    private fun notifyStatusTimerListeners(newStatus: Int) {
        statusTimerListeners.forEach { it(newStatus) }
    }
    private fun notifyTimerStartListeners(newStatus: Int) {
        timerStartListeners.forEach { it(newStatus) }
    }
    private fun notifyTimerEndListeners(newStatus: Int) {
        timerEndListeners.forEach { it(newStatus) }
    }
    private fun notifyRepeatListeners(new: String) {
        repeatListeners.forEach { it(new) }
    }

    private fun listenForChanges() {
        //lắng nghe sự thay đổi trạng thái máy bơm
        database.child("status").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val newStatus = snapshot.getValue(Int::class.java)
                newStatus?.let{
                    status =it
                    notifyStatusListeners(it)
                    Log.d("FirebaseData","Trạng thái máy bơm : $it")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("FirebaseError","Lỗi khi lấy trạng thái ",error.toException())
            }
        })

        //lắng nghe sự thay đổi trạng thái máy bơm tự động
        database.child("status_humidity_land").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val newStatus = snapshot.getValue(Int::class.java)
                newStatus?.let{
                    statusHumidityLand =it
                    notifyStatusHumidityLandListeners(it)
                    Log.d("FirebaseData","Trạng thái máy bơm tự đông : $it")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("FirebaseError","Lỗi khi lấy trạng thái ",error.toException())
            }
        })

        //lắng nghe sự thay đổi trạng thái máy bơm tự động
        database.child("status_timer").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val newStatus = snapshot.getValue(Int::class.java)
                newStatus?.let{
                    statusTimer =it
                    notifyStatusTimerListeners(it)
                    Log.d("FirebaseData","Trạng thái máy bơm hẹn giờ: $it")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("FirebaseError","Lỗi khi lấy trạng thái ",error.toException())
            }
        })
        database.child("timer_start").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val new = snapshot.getValue(Int::class.java)
                new?.let{
                    timerStart =it
                    notifyTimerStartListeners(it)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
        database.child("timer_end").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val new = snapshot.getValue(Int::class.java)
                new?.let{
                    timerEnd =it
                    notifyTimerEndListeners(it)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
        database.child("repeat").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val new = snapshot.getValue(String::class.java)
                new?.let{
                    repeat =it
                    notifyRepeatListeners(it)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    fun setWateringStatus(status: Int) {
        if (status == 0 || status == 1) { // Kiểm tra giá trị hợp lệ
            database.child("status").setValue(status)
                .addOnSuccessListener {
                    Log.d("FirebaseUpdate", "Cập nhật trạng thái tưới nước: $status")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Lỗi cập nhật", e)
                }
        } else {
            Log.e("FirebaseError", "Giá trị không hợp lệ: $status (chỉ chấp nhận 0 hoặc 1)")
        }
    }
    fun setWateringHumidityLand(humidityLand: Double) {
            database.child("humidity_land").setValue(humidityLand)
                .addOnSuccessListener {
                    Log.d("FirebaseUpdate", "Cập nhật độ ẩm cần tưới nước: $humidityLand")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Lỗi cập nhật", e)
                }
    }
    fun setWateringStatusHumidityLand(new :Int)
    {
        database.child("status_humidity_land").setValue(new)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Cập nhật : $new")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseError", "Lỗi cập nhật", e)
            }
    }
    fun setWateringStatusTimer(new :Int)
    {
        database.child("status_timer").setValue(new)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Cập nhật : $new")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseError", "Lỗi cập nhật", e)
            }
    }
    fun setWateringTimerEnd(new :Int)
    {
        database.child("timer_end").setValue(new)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Cập nhật  $new")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseError", "Lỗi cập nhật", e)
            }
    }
    fun setWateringTimerStart(new :Int)
    {
        database.child("timer_start").setValue(new)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Cập nhật : $status")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseError", "Lỗi cập nhật", e)
            }
    }
    fun setRepeat(new :String)
    {
        database.child("repeat").setValue(new)
            .addOnSuccessListener {
                Log.d("FirebaseUpdate", "Cập nhật : $new")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseError", "Lỗi cập nhật", e)
            }
    }
    fun getStatus() : Boolean
    {
        return status==1
    }
    fun getStatusHumidityLand() : Boolean
    {
        return statusHumidityLand==1
    }
    fun getStatusTimer() : Boolean
    {
        return statusTimer==1
    }
    fun getTimerStart() : Int?
    {
        return timerStart
    }fun getTimerEnd() : Int?
    {
        return timerEnd
    }
    fun getRepeat() : String?
    {
        return repeat
    }

}

