package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.data_pump
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

class DataPumpRepository (
) {
    private val database = FirebaseDatabase.getInstance()
    private val pumpRef = database.getReference("data_pump")

    companion object {
        private const val TAG = "PumpRepository"
    }

    fun getDataPumpList(): Flow<List<data_pump>> = callbackFlow {
        Log.d(TAG, "Starting to observe pump list")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val pumpList = mutableListOf<data_pump>()

                    for (childSnapshot in snapshot.children) {
                        val pump = childSnapshot.getValue(data_pump::class.java)
                        pump?.let {
                            pumpList.add(it)
                            Log.d(TAG, "Added pump status: ${it.status} at ${it.time}")
                        }
                    }

                    val sortedList = pumpList.sortedByDescending { it.time }
                    Log.d(TAG, "Sending ${sortedList.size} pump records")
                    trySend(sortedList)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing pump data", e)
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Pump listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        pumpRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing pump listener")
            pumpRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getDataPumpList", e)
        emit(emptyList())
    }.onStart {
        Log.d(TAG, "Flow started for pump list")
    }

    fun getLatestDataPumpStatus(): Flow<data_pump?> = callbackFlow {
        Log.d(TAG, "Starting to observe latest pump status")

        val latestQuery = pumpRef.orderByKey().limitToLast(1)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    var latestPump: data_pump? = null

                    if (snapshot.hasChildren()) {
                        val pumpList = mutableListOf<data_pump>()

                        for (childSnapshot in snapshot.children) {
                            val pump = childSnapshot.getValue(data_pump::class.java)
                            pump?.let { pumpList.add(it) }
                        }

                        latestPump = pumpList.maxByOrNull { it.time }
                    }

                    Log.d(TAG, "Latest pump status: ${latestPump?.status} at ${latestPump?.time}")
                    trySend(latestPump)

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing latest pump status", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Latest pump listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        latestQuery.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing latest pump listener")
            latestQuery.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getLatestDataPumpStatus", e)
        emit(null)
    }
    suspend fun deleteDataByDateRange(startDate: String, endDate: String): Boolean {
        return try {
            Log.d(TAG, "Bắt đầu xóa dữ liệu bơm từ $startDate đến $endDate")

            val snapshot = pumpRef.get().await()
            var deletedCount = 0

            for (childSnapshot in snapshot.children) {
                val pump = childSnapshot.getValue(data_pump::class.java)
                pump?.let {
                    if (isDateInRange(it.time, startDate, endDate)) {
                        childSnapshot.ref.removeValue().await()
                        deletedCount++
                        Log.d(TAG, "Đã xóa record bơm: ${it.time}")
                    }
                }
            }

            Log.d(TAG, "Xóa thành công $deletedCount records bơm")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xóa dữ liệu bơm", e)
            false
        }
    }

    suspend fun getDataByDateRange(startDate: String, endDate: String): List<data_pump> {
        return try {
            Log.d(TAG, "Lấy dữ liệu bơm từ $startDate đến $endDate")

            val snapshot = pumpRef.get().await()
            val filteredList = mutableListOf<data_pump>()

            for (childSnapshot in snapshot.children) {
                val pump = childSnapshot.getValue(data_pump::class.java)
                pump?.let {
                    if (isDateInRange(it.time, startDate, endDate)) {
                        filteredList.add(it)
                    }
                }
            }

            val sortedList = filteredList.sortedBy { it.time }
            Log.d(TAG, "Tìm thấy ${sortedList.size} records bơm trong khoảng thời gian")

            sortedList

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy dữ liệu bơm theo ngày", e)
            emptyList()
        }
    }

    private fun isDateInRange(dateTime: String, startDate: String, endDate: String): Boolean {
        return try {
            val dateOnly = dateTime.split(" ")[0]
            dateOnly in startDate..endDate
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi so sánh ngày bơm: $dateTime", e)
            false
        }
    }
}
