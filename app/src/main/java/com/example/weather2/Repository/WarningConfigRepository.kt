package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class WarningConfigRepository {
    private val database = FirebaseDatabase.getInstance()
    private val warningRef = database.getReference("local_warnings")

    companion object {
        private const val TAG = "WarningConfigRepository"
    }

    /**
     * Lắng nghe thay đổi warning config từ Firebase real-time
     */
    fun getWarningConfig(): Flow<E_WarningConfigFirebase?> = callbackFlow {
        Log.d(TAG, "Starting to observe warning config")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        val config = snapshot.getValue(E_WarningConfigFirebase::class.java)
                        Log.d(TAG, "Warning config updated: $config")
                        trySend(config)
                    } else {
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing warning config", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Warning config listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        warningRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing warning config listener")
            warningRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getWarningConfig", e)
        emit(null)
    }

    /**
     * Cập nhật warning config lên Firebase
     */
    suspend fun updateWarningConfig(config: E_WarningConfigFirebase): Result<Unit> {
        return try {
            Log.d(TAG, "Updating warning config: $config")
            warningRef.setValue(config).await()
            Log.d(TAG, "Warning config updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating warning config", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật trạng thái warning tổng thể
     */
    suspend fun updateWarningStatus(status: Int): Result<Int> {
        return try {
            val updates = mapOf("status" to status)
            warningRef.updateChildren(updates).await()
            Log.d(TAG, "Warning status updated successfully: $status")
            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating warning status", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật temperature thresholds
     */
    suspend fun updateTempThresholds(tempMax: Int, tempMin: Int): Result<Unit> {
        return try {
            val updates = mapOf(
                "tempMax" to tempMax,
                "tempMin" to tempMin
            )
            warningRef.updateChildren(updates).await()
            Log.d(TAG, "Temperature thresholds updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating temperature thresholds", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật humidity air thresholds
     */
    suspend fun updateHumidityAirThresholds(humidityMax: Int, humidityMin: Int): Result<Unit> {
        return try {
            val updates = mapOf(
                "humidityAirMax" to humidityMax,
                "humidityAirMin" to humidityMin
            )
            warningRef.updateChildren(updates).await()
            Log.d(TAG, "Humidity air thresholds updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating humidity air thresholds", e)
            Result.failure(e)
        }
    }
}