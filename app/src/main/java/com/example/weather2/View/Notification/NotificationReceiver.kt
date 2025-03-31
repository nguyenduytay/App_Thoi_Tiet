package com.example.weather2.View.Notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.weather2.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Thông báo mặc định"
        val intervalMillis = intent.getLongExtra("intervalMillis", 60000L) // Mặc định 1 phút

        // Hiển thị thông báo
        NotificationHelper(context).showNotification(message)

        // Lên lịch lại thông báo
        NotificationHelper(context).scheduleNotification(context, intervalMillis, message)
    }
    
}


