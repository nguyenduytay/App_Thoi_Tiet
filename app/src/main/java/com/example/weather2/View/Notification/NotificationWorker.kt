package com.example.weather2.View.Notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val message = inputData.getString("message") ?: return Result.failure()
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showNotification(message)
        return Result.success()
    }
}
