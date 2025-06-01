package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.data_humidity
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

class DataHumidityRepository() {

    private val database = FirebaseDatabase.getInstance()
    private val humidityRef = database.getReference("data_humidity")

    companion object {
        private const val TAG = "HumidityRepository"
    }

    fun getDataHumidityList(): Flow<List<data_humidity>> = callbackFlow {
        Log.d(TAG, "Starting to observe humidity list")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val humidityList = mutableListOf<data_humidity>()

                    for (childSnapshot in snapshot.children) {
                        val humidity = childSnapshot.getValue(data_humidity::class.java)
                        humidity?.let {
                            humidityList.add(it)
                            Log.d(TAG, "Added humidity: ${it.humidity}% at ${it.time}")
                        }
                    }

                    // Sắp xếp theo thời gian (mới nhất trước)
                    val sortedList = humidityList.sortedByDescending { it.time }
                    Log.d(TAG, "Sending ${sortedList.size} humidity records")
                    trySend(sortedList)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing humidity data", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        humidityRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing humidity listener")
            humidityRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getDataHumidityList", e)
        emit(emptyList())
    }.onStart {
        Log.d(TAG, "Flow started for humidity list")
    }

    fun getLatestDataHumidity(): Flow<data_humidity?> = callbackFlow {
        Log.d(TAG, "Starting to observe latest humidity")

        // Tối ưu: Chỉ lấy 1 record mới nhất thay vì toàn bộ list
        val latestQuery = humidityRef.orderByKey().limitToLast(1)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    var latestHumidity: data_humidity? = null

                    // Nếu có nhiều records, tìm cái mới nhất
                    if (snapshot.hasChildren()) {
                        val humidityList = mutableListOf<data_humidity>()

                        for (childSnapshot in snapshot.children) {
                            val humidity = childSnapshot.getValue(data_humidity::class.java)
                            humidity?.let { humidityList.add(it) }
                        }

                        latestHumidity = humidityList.maxByOrNull { it.time }
                    }

                    Log.d(TAG, "Latest humidity: ${latestHumidity?.humidity}% at ${latestHumidity?.time}")
                    trySend(latestHumidity)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing latest humidity", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Latest humidity listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        latestQuery.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing latest humidity listener")
            latestQuery.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getLatestDataHumidity", e)
        emit(null)
    }

    /**
     * Xóa dữ liệu theo khoảng thời gian
     */
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            Log.d(TAG, "Bắt đầu xóa dữ liệu độ ẩm từ $startDate đến $endDate")

            // Lấy tất cả dữ liệu trước để filter theo thời gian
            val snapshot = humidityRef.get().await()
            var deletedCount = 0

            for (childSnapshot in snapshot.children) {
                val humidity = childSnapshot.getValue(data_humidity::class.java)
                humidity?.let {
                    // Kiểm tra nếu thời gian nằm trong khoảng cần xóa
                    if (isDateInRange(it.time, startDate, endDate)) {
                        // Xóa record này
                        childSnapshot.ref.removeValue().await()
                        deletedCount++
                        Log.d(TAG, "Đã xóa record: ${it.time}")
                    }
                }
            }

            Log.d(TAG, "Xóa thành công $deletedCount records độ ẩm")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xóa dữ liệu độ ẩm", e)
            false
        }
    }

    /**
     * Lấy dữ liệu theo khoảng thời gian
     */
    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_humidity> {
        return try {
            Log.d(TAG, "Lấy dữ liệu độ ẩm từ $startDate đến $endDate")

            val snapshot = humidityRef.get().await()
            val filteredList = mutableListOf<data_humidity>()

            for (childSnapshot in snapshot.children) {
                val humidity = childSnapshot.getValue(data_humidity::class.java)
                humidity?.let {
                    if (isDateInRange(it.time, startDate, endDate)) {
                        filteredList.add(it)
                    }
                }
            }

            // Sắp xếp theo thời gian tăng dần
            val sortedList = filteredList.sortedBy { it.time }
            Log.d(TAG, "Tìm thấy ${sortedList.size} records độ ẩm trong khoảng thời gian")

            sortedList

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy dữ liệu độ ẩm theo ngày", e)
            emptyList()
        }
    }

    /**
     * Kiểm tra xem một ngày có nằm trong khoảng thời gian không
     */
    private fun isDateInRange(dateTime: String, startDate: String, endDate: String): Boolean {
        return try {
            // dateTime format: "2025-05-26 20:58:51"
            // startDate/endDate format: "2025-05-26"

            val dateOnly = dateTime.split(" ")[0] // Lấy phần ngày, bỏ phần giờ
            dateOnly in startDate..endDate

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi so sánh ngày: $dateTime", e)
            false
        }
    }
}