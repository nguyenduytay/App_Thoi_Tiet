package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.E_Weather7dFirebase
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class Weather7dRepository {
    private val database = FirebaseDatabase.getInstance()
    private val weather7dRef = database.getReference("weather_7d")

    companion object {
        private const val TAG = "Weather7dRepository"
    }

    fun get7DayWeatherData(): Flow<Map<String, E_Weather7dFirebase>> = callbackFlow {
        Log.d(TAG, "Starting to observe 7d weather data")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val weatherMap = mutableMapOf<String, E_Weather7dFirebase>()

                    for (daySnapshot in snapshot.children) {
                        val day = daySnapshot.key
                        val data = daySnapshot.getValue(E_Weather7dFirebase::class.java)

                        if (day != null && data != null) {
                            weatherMap[day] = data
                        }
                    }

                    Log.d(TAG, "7d weather data updated: ${weatherMap.size} entries")
                    trySend(weatherMap.toMap())
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 7d weather data", e)
                    trySend(emptyMap())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "7d weather listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        weather7dRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing 7d weather listener")
            weather7dRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in get7DayWeatherData", e)
        emit(emptyMap())
    }

    suspend fun update7DayWeatherData(weatherMap: Map<String, E_Weather7dFirebase>): Result<Unit> {
        return try {
            Log.d(TAG, "Updating 7d weather data: ${weatherMap.size} entries")
            weather7dRef.setValue(weatherMap).await()
            Log.d(TAG, "7d weather data updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating 7d weather data", e)
            Result.failure(e)
        }
    }
}