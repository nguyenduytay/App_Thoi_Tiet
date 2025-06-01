package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.data_rain
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

class DataRainRepository(
) {
    private val database = FirebaseDatabase.getInstance()
    private val rainRef = database.getReference("data_rain")

    companion object {
        private const val TAG = "RainRepository"
    }

    fun getDataRainList(): Flow<List<data_rain>> = callbackFlow {
        Log.d(TAG, "Starting to observe rain list")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val rainList = mutableListOf<data_rain>()

                    for (childSnapshot in snapshot.children) {
                        val rain = childSnapshot.getValue(data_rain::class.java)
                        rain?.let {
                            rainList.add(it)
                            Log.d(TAG, "Added rain: ${it.rain}% at ${it.time}")
                        }
                    }

                    val sortedList = rainList.sortedByDescending { it.time }
                    Log.d(TAG, "Sending ${sortedList.size} rain records")
                    trySend(sortedList)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing rain data", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Rain listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        rainRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing rain listener")
            rainRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getDataRainList", e)
        emit(emptyList())
    }.onStart {
        Log.d(TAG, "Flow started for rain list")
    }

    fun getLatestDataRain(): Flow<data_rain?> = callbackFlow {
        Log.d(TAG, "Starting to observe latest rain")

        val latestQuery = rainRef.orderByKey().limitToLast(1)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    var latestRain: data_rain? = null

                    if (snapshot.hasChildren()) {
                        val rainList = mutableListOf<data_rain>()

                        for (childSnapshot in snapshot.children) {
                            val rain = childSnapshot.getValue(data_rain::class.java)
                            rain?.let { rainList.add(it) }
                        }

                        latestRain = rainList.maxByOrNull { it.time }
                    }

                    Log.d(TAG, "Latest rain: ${latestRain?.rain}% at ${latestRain?.time}")
                    trySend(latestRain)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing latest rain", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Latest rain listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        latestQuery.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing latest rain listener")
            latestQuery.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getLatestDataRain", e)
        emit(null)
    }
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            Log.d(TAG, "Bắt đầu xóa dữ liệu mưa từ $startDate đến $endDate")

            val snapshot = rainRef.get().await()
            var deletedCount = 0

            for (childSnapshot in snapshot.children) {
                val rain = childSnapshot.getValue(data_rain::class.java)
                rain?.let {
                    if (isDateInRange(it.time, startDate, endDate)) {
                        childSnapshot.ref.removeValue().await()
                        deletedCount++
                        Log.d(TAG, "Đã xóa record mưa: ${it.time}")
                    }
                }
            }

            Log.d(TAG, "Xóa thành công $deletedCount records mưa")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xóa dữ liệu mưa", e)
            false
        }
    }

    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_rain> {
        return try {
            Log.d(TAG, "Lấy dữ liệu mưa từ $startDate đến $endDate")

            val snapshot = rainRef.get().await()
            val filteredList = mutableListOf<data_rain>()

            for (childSnapshot in snapshot.children) {
                val rain = childSnapshot.getValue(data_rain::class.java)
                rain?.let {
                    if (isDateInRange(it.time, startDate, endDate)) {
                        filteredList.add(it)
                    }
                }
            }

            val sortedList = filteredList.sortedBy { it.time }
            Log.d(TAG, "Tìm thấy ${sortedList.size} records mưa trong khoảng thời gian")

            sortedList

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy dữ liệu mưa theo ngày", e)
            emptyList()
        }
    }

    private fun isDateInRange(dateTime: String, startDate: String, endDate: String): Boolean {
        return try {
            val dateOnly = dateTime.split(" ")[0]
            dateOnly in startDate..endDate
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi so sánh ngày mưa: $dateTime", e)
            false
        }
    }
}