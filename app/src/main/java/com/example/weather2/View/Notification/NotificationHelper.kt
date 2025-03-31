//package com.example.weather2.View.Notification
//
//import android.annotation.SuppressLint
//import android.app.AlarmManager
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.Handler
//import android.os.Looper
//import androidx.core.app.NotificationCompat
//
//class NotificationHelper(private val context: Context) {
//
//    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//    private val handler = Handler(Looper.getMainLooper())
//    private val notificationId = 100
//    private val channelId = "notification_channel"
//
//    init {
//        createNotificationChannel()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(channelId, "Notification Channel", NotificationManager.IMPORTANCE_HIGH)
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    fun showNotification(message: String) {
//        val notification = NotificationCompat.Builder(context, channelId)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentTitle("Thông báo")
//            .setContentText(message)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .build()
//        notificationManager.notify(notificationId, notification)
//    }
//
//    // 1. Gửi thông báo lặp lại sau một khoảng thời gian (milliseconds)
//    fun scheduleRepeatingNotification(message: String, intervalMillis: Long) {
//        handler.postDelayed(object : Runnable {
//            override fun run() {
//                showNotification(message)
//                handler.postDelayed(this, intervalMillis)
//            }
//        }, intervalMillis)
//    }
//
//    // 2. Gửi thông báo vào đúng thời gian thực tế
//    @SuppressLint("ScheduleExactAlarm")
//    fun scheduleNotificationAtExactTime(message: String, triggerTimeMillis: Long) {
//        val intent = Intent(context, NotificationReceiver::class.java).apply {
//            putExtra("message", message)
//        }
//        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
//    }
//
//    // 3. Cảnh báo nếu dữ liệu vượt quá ngưỡng
//    fun checkThresholdMax(value: Double, threshold: Double, message: String) {
//        if (value > threshold) {
//            showNotification("Cảnh báo: $message - Giá trị: $value cao hơn : $threshold")
//        }
//    }
//    fun checkThresholdMin(value: Double, threshold: Double, message: String) {
//        if (value < threshold) {
//            showNotification("Cảnh báo: $message - Giá trị: $value thấp hơn : $threshold")
//        }
//    }
//}
//
//

package com.example.weather2.View.Notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.weather2.MainActivity
import com.example.weather2.R
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private val notificationId = 100
    private val channelId = "notification_channel"
    private var notificationRunnable: Runnable? = null
    private val notificationRunnables = mutableListOf<Runnable>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notification Channel", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
    }
    @SuppressLint("NotificationPermission")
    fun showNotification(message: String) {
        // 1. Tạo Intent mở Activity chính
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true) // Có thể thêm extra nếu cần
        }

        // 2. Tạo PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Xây dựng thông báo
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.cloudy_sunny)
            .setContentTitle("Thông báo thời tiết")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Quan trọng: Gán intent mở app
            .setAutoCancel(true) // Tự động đóng thông báo khi nhấn vào
            .build()

        notificationManager.notify(notificationId, notification)
    }
    // 1. Gửi thông báo lặp lại sau một khoảng thời gian (milliseconds)
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(context: Context, intervalMillis: Long, message: String) {
        // Kiểm tra quyền trên Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                // Yêu cầu quyền nếu chưa có
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("message", message)
            putExtra("intervalMillis", intervalMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            pendingIntent
        )
    }
    //1.1 hủy
    fun cancelScheduledNotification(context: Context) {
        try {
            // 1. Tạo Intent GIỐNG HỆT với khi lên lịch
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                // Lưu ý: KHÔNG thêm action nếu scheduleNotification không dùng action
                putExtra("message", "") // Cùng key nhưng value không quan trọng
                putExtra("intervalMillis", 0L)
            }

            // 2. Tạo PendingIntent với cùng tham số
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0, // PHẢI GIỐNG requestCode khi lên lịch (ở đây là 0)
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Flag quan trọng
            )

            // 3. Hủy nếu tồn tại
            pendingIntent?.let {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                alarmManager.cancel(it)
                it.cancel()
                Log.d("Notification", "Đã hủy thông báo đã lên lịch")
            } ?: Log.w("Notification", "Không tìm thấy thông báo để hủy")
        } catch (e: Exception) {
            Log.e("Notification", "Lỗi khi hủy thông báo", e)
        }
    }
    //1.2 hàm phóng to thông báo
    fun showBigStyleNotification(title: String, bigText: String, summaryText: String) {
        // Intent mở app khi nhấn vào thông báo
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo thông báo dạng mở rộng
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(bigText)
            .setBigContentTitle(title)
            .setSummaryText(summaryText)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notifications)
            .setContentTitle(title)
            .setContentText(bigText.lines().firstOrNull() ?: "") // Hiển thị dòng đầu tiên ở notification nhỏ
            .setStyle(bigTextStyle) // Áp dụng style mở rộng
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Random.nextInt(), notification)
    }
    // 2. Gửi thông báo vào đúng thời gian thực tế bằng WorkManager
    fun scheduleNotificationAtExactTime(message: String, triggerTimeMillis: Long) {
        val delay = triggerTimeMillis - System.currentTimeMillis()
        if (delay <= 0) return // Tránh đặt lịch nếu thời gian đã trôi qua

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("message" to message))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
