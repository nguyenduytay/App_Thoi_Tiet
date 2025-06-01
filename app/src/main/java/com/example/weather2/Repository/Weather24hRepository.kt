package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.E_Weather24hFirebase
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class Weather24hRepository {
    private val database = FirebaseDatabase.getInstance()
    private val weather24hRef = database.getReference("weather_24h")

    companion object {
        private const val TAG = "Weather24hRepository"
    }

    fun get24HourWeatherData(): Flow<Map<String, E_Weather24hFirebase>> = callbackFlow {
        Log.d(TAG, "Starting to observe 24h weather data")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val weatherMap = mutableMapOf<String, E_Weather24hFirebase>()

                    for (timeSnapshot in snapshot.children) {
                        val time = timeSnapshot.key
                        val data = timeSnapshot.getValue(E_Weather24hFirebase::class.java)

                        if (time != null && data != null) {
                            weatherMap[time] = data
                        }
                    }

                    Log.d(TAG, "24h weather data updated: ${weatherMap.size} entries")
                    trySend(weatherMap.toMap())
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 24h weather data", e)
                    trySend(emptyMap())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "24h weather listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        weather24hRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing 24h weather listener")
            weather24hRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in get24HourWeatherData", e)
        emit(emptyMap())
    }

    suspend fun update24HourWeatherData(weatherMap: Map<String, E_Weather24hFirebase>): Result<Unit> {
        return try {
            Log.d(TAG, "Updating 24h weather data: ${weatherMap.size} entries")
            weather24hRef.setValue(weatherMap).await()
            Log.d(TAG, "24h weather data updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating 24h weather data", e)
            Result.failure(e)
        }
    }
}