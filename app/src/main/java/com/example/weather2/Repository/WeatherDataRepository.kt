package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.E_WeatherDataFirebase
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class WeatherDataRepository {
    private val database = FirebaseDatabase.getInstance()
    private val weatherRef = database.getReference("weather_data")

    companion object {
        private const val TAG = "WeatherDataRepository"
    }

    fun getCurrentWeatherData(): Flow<E_WeatherDataFirebase?> = callbackFlow {
        Log.d(TAG, "Starting to observe current weather data")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val weatherData = snapshot.getValue(E_WeatherDataFirebase::class.java)
                    Log.d(TAG, "Current weather: temp=${weatherData?.temperature}°C, humidity=${weatherData?.humidity}%")
                    trySend(weatherData)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing weather data", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Weather listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        weatherRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing weather listener")
            weatherRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getCurrentWeatherData", e)
        emit(null)
    }

    suspend fun updateWeatherData(weatherData: E_WeatherDataFirebase): Result<Unit> {
        return try {
            Log.d(TAG, "Updating weather data: temp=${weatherData.temperature}°C")
            weatherRef.setValue(weatherData).await()
            Log.d(TAG, "Weather data updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating weather data", e)
            Result.failure(e)
        }
    }

    suspend fun updatePumpStatus(isOn: Boolean): Result<Boolean> {
        return try {
            val updates = mapOf("status_pump" to isOn)
            weatherRef.updateChildren(updates).await()
            Log.d(TAG, "Pump status updated successfully: $isOn")
            Result.success(isOn)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pump status", e)
            Result.failure(e)
        }
    }

    suspend fun updateBlindStatus(isOn: Boolean): Result<Boolean> {
        return try {
            val updates = mapOf("status_blind" to isOn)
            weatherRef.updateChildren(updates).await()
            Log.d(TAG, "Blind status updated successfully: $isOn")
            Result.success(isOn)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating blind status", e)
            Result.failure(e)
        }
    }
}
