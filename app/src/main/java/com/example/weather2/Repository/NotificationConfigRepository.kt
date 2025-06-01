package com.example.weather2.Repository

import android.util.Log
import com.example.weather2.Model.Entity.E_NotificationConfigFirebase
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class NotificationConfigRepository {
    private val database = FirebaseDatabase.getInstance()
    private val notificationRef = database.getReference("local_notification")

    companion object {
        private const val TAG = "NotificationConfigRepository"
    }

    /**
     * Lắng nghe thay đổi notification config từ Firebase real-time
     */
    fun getNotificationConfig(): Flow<E_NotificationConfigFirebase?> = callbackFlow {
        Log.d(TAG, "Starting to observe notification config")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        val config = snapshot.getValue(E_NotificationConfigFirebase::class.java)
                        Log.d(TAG, "Notification config updated: $config")
                        trySend(config)
                    } else {
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notification config", e)
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Notification config listener cancelled: ${error.message}")
                close(error.toException())
            }
        }

        notificationRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "Removing notification config listener")
            notificationRef.removeEventListener(listener)
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getNotificationConfig", e)
        emit(null)
    }

    /**
     * Cập nhật notification config lên Firebase
     */
    suspend fun updateNotificationConfig(config: E_NotificationConfigFirebase): Result<Unit> {
        return try {
            Log.d(TAG, "Updating notification config: $config")
            notificationRef.setValue(config).await()
            Log.d(TAG, "Notification config updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification config", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật trạng thái notification
     */
    suspend fun updateNotificationStatus(status: Boolean): Result<Boolean> {
        return try {
            val updates = mapOf("status" to status)
            notificationRef.updateChildren(updates).await()
            Log.d(TAG, "Notification status updated successfully: $status")
            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification status", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật thời gian notification
     */
    suspend fun updateNotificationTime(time: Int): Result<Int> {
        return try {
            val updates = mapOf("time" to time)
            notificationRef.updateChildren(updates).await()
            Log.d(TAG, "Notification time updated successfully: $time")
            Result.success(time)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification time", e)
            Result.failure(e)
        }
    }
}