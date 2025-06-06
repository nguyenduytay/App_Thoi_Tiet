package com.example.weather2.Notification

import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {
    private var fcmToken: String? = null
    fun fetchToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                fcmToken = token
            }
            .addOnFailureListener {
                fcmToken = null
            }
    }
    fun getToken(onTokenReceived: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                onTokenReceived(token)
            }
            .addOnFailureListener { e ->
                onTokenReceived(null)
            }
    }
}

