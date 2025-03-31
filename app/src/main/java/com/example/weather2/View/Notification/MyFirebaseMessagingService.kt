package com.example.weather2.View.Notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.weather2.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {
    // Nhận thông báo khi app đang chạy
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Kiểm tra nếu thông báo chứa payload dữ liệu
        if (remoteMessage.data.isNotEmpty()) {
            // Xử lý dữ liệu tùy chỉnh nếu có
            val customData = remoteMessage.data["custom_key"] // Thay "custom_key" bằng key bạn cần xử lý
        }
        // Kiểm tra nếu thông báo chứa notification
        remoteMessage.notification?.let {
            val title = it.title
            val body = it.body
            sendNotification(title, body)
        }
    }
    private fun sendNotification(title: String?, body: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Tạo notification
        val notification = NotificationCompat.Builder(this, "default")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.cloudy_sunny) // Thêm icon phù hợp
            .setAutoCancel(true)
        // Kiểm tra Android phiên bản để sử dụng channel notification (Android 8.0 trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default", "Default", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notification.build())
    }
}



