package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.E_WateringFirebase
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class WateringRepository {
    private val database = FirebaseDatabase.getInstance()
    private val wateringRef = database.getReference("watering")

    companion object {
        private const val TAG = "WateringRepository"
    }

    /**
     * Lắng nghe thay đổi watering data từ Firebase real-time
     */
    fun getWateringData(): Flow<E_WateringFirebase?> = callbackFlow {
        Log.d(TAG, "Starting to observe watering data")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        val data = snapshot.getValue(E_WateringFirebase::class.java)
                        Log.d(TAG, "Watering data updated: $data")
                        trySend(data)
                    } else {
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing watering data", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Watering data listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        wateringRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing watering data listener")
            wateringRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getWateringData", e)
        emit(null)
    }

    /**
     * Cập nhật watering data lên Firebase
     */
    suspend fun updateWateringData(data: E_WateringFirebase): Result<Unit> {
        return try {
            Log.d(TAG, "Updating watering data: $data")
            wateringRef.setValue(data).await()
            Log.d(TAG, "Watering data updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating watering data", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật một field cụ thể
     */
    suspend fun updateField(fieldName: String, value: Any): Result<Unit> {
        return try {
            wateringRef.child(fieldName).setValue(value).await()
            Log.d(TAG, "Successfully updated $fieldName: $value")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating $fieldName", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật trạng thái watering (0 hoặc 1)
     */
    suspend fun setWateringStatus(status: Int): Result<Int> {
        return if (status == 0 || status == 1) {
            updateField("status", status).map { status }
        } else {
            Log.e(TAG, "Invalid watering status: $status (only 0 or 1 accepted)")
            Result.failure(IllegalArgumentException("Invalid status: $status"))
        }
    }

    /**
     * Cập nhật timer start và end
     */
    suspend fun updateWateringTimer(timerStart: Int, timerEnd: Int): Result<Unit> {
        return try {
            val updates = mapOf(
                "timer_start" to timerStart,
                "timer_end" to timerEnd
            )
            wateringRef.updateChildren(updates).await()
            Log.d(TAG, "Watering timer updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating watering timer", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật humidity land thresholds
     */
    suspend fun updateHumidityLandThresholds(humidityMax: Int, humidityMin: Int): Result<Unit> {
        return try {
            val updates = mapOf(
                "humidity_land_max" to humidityMax,
                "humidity_land_min" to humidityMin
            )
            wateringRef.updateChildren(updates).await()
            Log.d(TAG, "Humidity land thresholds updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating humidity land thresholds", e)
            Result.failure(e)
        }
    }
}