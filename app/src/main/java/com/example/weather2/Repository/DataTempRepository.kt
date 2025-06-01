package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.data_temp
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

class DataTempRepository() {

    private val database = FirebaseDatabase.getInstance()
    private val tempRef = database.getReference("data_temp")

    companion object {
        private const val TAG = "TempRepository"
    }

    fun getDataTempList(): Flow<List<data_temp>> = callbackFlow {
        Log.d(TAG, "Starting to observe temperature list")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val tempList = mutableListOf<data_temp>()

                    for (childSnapshot in snapshot.children) {
                        val temp = childSnapshot.getValue(data_temp::class.java)
                        temp?.let {
                            tempList.add(it)
                            Log.d(TAG, "Added temperature: ${it.temp}°C at ${it.time}")
                        }
                    }

                    // Sắp xếp theo thời gian (mới nhất trước)
                    val sortedList = tempList.sortedByDescending { it.time }
                    Log.d(TAG, "Sending ${sortedList.size} temperature records")
                    trySend(sortedList)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing temperature data", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        tempRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing temperature listener")
            tempRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getDataTempList", e)
        emit(emptyList())
    }.onStart {
        Log.d(TAG, "Flow started for temperature list")
    }

    fun getLatestDataTemp(): Flow<data_temp?> = callbackFlow {
        Log.d(TAG, "Starting to observe latest temperature")

        // Tối ưu: Chỉ lấy 1 record mới nhất thay vì toàn bộ list
        val latestQuery = tempRef.orderByKey().limitToLast(1)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    var latestTemp: data_temp? = null

                    // Nếu có nhiều records, tìm cái mới nhất
                    if (snapshot.hasChildren()) {
                        val tempList = mutableListOf<data_temp>()

                        for (childSnapshot in snapshot.children) {
                            val temp = childSnapshot.getValue(data_temp::class.java)
                            temp?.let { tempList.add(it) }
                        }

                        latestTemp = tempList.maxByOrNull { it.time }
                    }

                    Log.d(TAG, "Latest temperature: ${latestTemp?.temp}°C at ${latestTemp?.time}")
                    trySend(latestTemp)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing latest temperature", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Latest temperature listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        latestQuery.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing latest temperature listener")
            latestQuery.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getLatestDataTemp", e)
        emit(null)
    }

    /**
     * Xóa dữ liệu theo khoảng thời gian
     */
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            Log.d(TAG, "Bắt đầu xóa dữ liệu nhiệt độ từ $startDate đến $endDate")

            // Lấy tất cả dữ liệu trước để filter theo thời gian
            val snapshot = tempRef.get().await()
            var deletedCount = 0

            for (childSnapshot in snapshot.children) {
                val temp = childSnapshot.getValue(data_temp::class.java)
                temp?.let {
                    // Kiểm tra nếu thời gian nằm trong khoảng cần xóa
                    if (isDateInRange(it.time, startDate, endDate)) {
                        // Xóa record này
                        childSnapshot.ref.removeValue().await()
                        deletedCount++
                        Log.d(TAG, "Đã xóa record: ${it.time}")
                    }
                }
            }

            Log.d(TAG, "Xóa thành công $deletedCount records nhiệt độ")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xóa dữ liệu nhiệt độ", e)
            false
        }
    }

    /**
     * Lấy dữ liệu theo khoảng thời gian
     */
    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_temp> {
        return try {
            Log.d(TAG, "Lấy dữ liệu nhiệt độ từ $startDate đến $endDate")

            val snapshot = tempRef.get().await()
            val filteredList = mutableListOf<data_temp>()

            for (childSnapshot in snapshot.children) {
                val temp = childSnapshot.getValue(data_temp::class.java)
                temp?.let {
                    if (isDateInRange(it.time, startDate, endDate)) {
                        filteredList.add(it)
                    }
                }
            }

            // Sắp xếp theo thời gian tăng dần
            val sortedList = filteredList.sortedBy { it.time }
            Log.d(TAG, "Tìm thấy ${sortedList.size} records nhiệt độ trong khoảng thời gian")

            sortedList

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy dữ liệu nhiệt độ theo ngày", e)
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
            dateOnly >= startDate && dateOnly <= endDate

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi so sánh ngày: $dateTime", e)
            false
        }
    }
}