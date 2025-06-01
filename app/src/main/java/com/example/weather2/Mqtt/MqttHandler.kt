package com.example.weather2.Mqtt

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import android.os.Handler
import android.os.Looper
import java.util.UUID

/**
 * MqttHandler để gửi lệnh điều khiển tới ESP32
 * Chỉ gửi dữ liệu đi, không nhận dữ liệu về từ ESP32
 * Sử dụng topics tương thích với code ESP32
 */
class MqttHandler(private val context: Context) {

    // =================================
    // CONSTANTS - Hằng số kết nối
    // =================================
    companion object {
        private const val TAG = "MqttHandler"
        private const val SERVER_URI = "ssl://9f86891678dd45ea9131f5abca3db44e.s1.eu.hivemq.cloud"
        private const val USERNAME = "tayduy"
        private const val PASSWORD = "Tay2004x8"
        private const val PREFS_NAME = "mqtt_preferences"
        private const val CLIENT_ID_KEY = "client_id"
    }

    // =================================
    // MQTT TOPICS - Đồng bộ với ESP32
    // =================================
    object Topics {
        // Topics cho máy bơm (watering system)
        const val STATUS_PUMP = "weather/status_pump"                    // Bật/tắt máy bơm thủ công
        const val MODE_AUTO = "weather/mode_auto"                        // Chế độ tự động máy bơm
        const val HUMIDITY_LAND_MAX = "weather/auto/humidity_land_max"   // Ngưỡng độ ẩm tối đa
        const val HUMIDITY_LAND_MIN = "weather/auto/humidity_land_min"   // Ngưỡng độ ẩm tối thiểu

        // Topics cho rèm cửa (window blinds)
        const val STATUS_BLIND = "weather/status_blind"                  // Bật/tắt rèm cửa thủ công
    }

    // =================================
    // MQTT CLIENT PROPERTIES
    // =================================
    private var mqttClient: MqttClient? = null // MQTT client instance

    // =================================
    // COROUTINE MANAGEMENT
    // =================================
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job()) // Coroutine scope cho async operations

    // =================================
    // CONNECTION STATE
    // =================================
    @Volatile
    private var isConnected = false // Thread-safe connection status

    // =================================
    // PREFERENCES - Lưu trữ Client ID
    // =================================
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // =================================
    // CALLBACK INTERFACE
    // =================================
    private var mqttCallback: MqttCallback? = null // Callback để nhận events

    /**
     * Interface callback cho các sự kiện MQTT
     */
    interface MqttCallback {
        fun onConnected() // Được gọi khi kết nối thành công
        fun onDisconnected() // Được gọi khi mất kết nối
        fun onMessageReceived(topic: String, message: String) // Được gọi khi nhận message (không dùng)
        fun onConnectionFailed(exception: Throwable?) // Được gọi khi kết nối thất bại
    }

    /**
     * Thiết lập callback để nhận thông báo về trạng thái kết nối
     */
    fun setCallback(callback: MqttCallback) {
        this.mqttCallback = callback
    }

    // =================================
    // SSL CONFIGURATION - Cấu hình SSL
    // =================================

    /**
     * Tạo SSL context để kết nối an toàn với MQTT broker
     */
    private fun createSslContext(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Trust all certificates - chỉ dùng cho development
            }
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Trust all certificates - chỉ dùng cho development
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        return SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom()) // Khởi tạo với trust manager
        }
    }

    // =================================
    // CLIENT ID MANAGEMENT - Quản lý Client ID
    // =================================

    /**
     * Tạo hoặc lấy Client ID duy nhất từ SharedPreferences
     */
    private fun getClientId(): String {
        // Kiểm tra Client ID đã lưu
        var clientId = sharedPreferences.getString(CLIENT_ID_KEY, null)

        // Tạo mới nếu chưa có
        if (clientId == null) {
            clientId = "AndroidClient-${UUID.randomUUID().toString().substring(0, 8)}"
            sharedPreferences.edit().putString(CLIENT_ID_KEY, clientId).apply() // Lưu vào preferences
            Log.d(TAG, "Tạo Client ID mới: $clientId")
        } else {
            Log.d(TAG, "Sử dụng Client ID đã lưu: $clientId")
        }

        return clientId
    }

    /**
     * Reset Client ID để buộc tạo mới trong lần kết nối tiếp theo
     */
    fun resetClientId() {
        sharedPreferences.edit().remove(CLIENT_ID_KEY).apply() // Xóa khỏi preferences
        Log.d(TAG, "Đã reset Client ID")
    }

    // =================================
    // CONNECTION METHODS - Các phương thức kết nối
    // =================================

    /**
     * Kết nối tới MQTT broker
     */
    fun connect() {
        coroutineScope.launch { // Launch trong coroutine
            try {
                // Ngắt kết nối cũ nếu còn
                if (mqttClient?.isConnected == true) {
                    try {
                        mqttClient?.disconnect() // Disconnect client cũ
                        Log.d(TAG, "Ngắt kết nối cũ trước khi kết nối lại")
                    } catch (e: MqttException) {
                        Log.e(TAG, "Lỗi khi ngắt kết nối cũ", e)
                    }
                }

                // Cấu hình kết nối MQTT
                val options = MqttConnectOptions().apply {
                    isCleanSession = true // Quan trọng: dùng clean session để tránh conflict
                    connectionTimeout = 30 // Timeout 30 giây
                    keepAliveInterval = 60 // Keep alive 60 giây
                    userName = USERNAME // Set username
                    password = PASSWORD.toCharArray() // Set password

                    // Cấu hình SSL cho connection an toàn
                    socketFactory = createSslContext().socketFactory
                    isAutomaticReconnect = true // Tự động reconnect khi mất kết nối
                }

                // Tạo MQTT client với ID duy nhất
                val clientId = getClientId()
                mqttClient = MqttClient(SERVER_URI, clientId, MemoryPersistence())
                Log.d(TAG, "Đang kết nối MQTT với ID: $clientId")

                // Thiết lập callback để xử lý events
                mqttClient?.setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        isConnected = false // Update connection status
                        Log.e(TAG, "Mất kết nối MQTT", cause)

                        // Chuyển sang Main thread để callback
                        Handler(Looper.getMainLooper()).post {
                            mqttCallback?.onDisconnected()
                        }

                        // Thử kết nối lại sau 5 giây
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!isConnected) {
                                connect() // Reconnect
                            }
                        }, 5000)
                    }

                    @Throws(Exception::class)
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        // KHÔNG XỬ LÝ MESSAGE NHẬN VỀ - chỉ gửi đi
                        // Vì app chỉ gửi lệnh điều khiển, không nhận dữ liệu từ ESP32
                        topic?.let { topicName ->
                            message?.let { mqttMessage ->
                                val messageStr = String(mqttMessage.payload)
                                Log.d(TAG, "Nhận tin nhắn (bỏ qua): $topicName - $messageStr")
                            }
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(TAG, "Gửi tin nhắn thành công") // Xác nhận gửi thành công
                    }
                })

                // Thực hiện kết nối
                mqttClient?.connect(options)

                // NOTE: KHÔNG SUBSCRIBE VÀO BẤT KỲ TOPIC NÀO
                // Vì app chỉ gửi lệnh điều khiển, không cần nhận dữ liệu

                // Cập nhật trạng thái kết nối
                isConnected = true

                // Thông báo kết nối thành công
                withContext(Dispatchers.Main) {
                    mqttCallback?.onConnected()
                }

                Log.d(TAG, "Kết nối MQTT thành công - chế độ chỉ gửi")

            } catch (e: Exception) {
                isConnected = false
                Log.e(TAG, "Lỗi kết nối MQTT: ${e.message}", e)

                // Xử lý lỗi Client ID conflict
                if (e is MqttException) {
                    val reason = e.reasonCode
                    Log.d(TAG, "MQTT Error code: $reason")

                    // Codes: 32100 = Client ID in use, 32103 = Client already connected
                    if (reason == 32100 || reason == 32103) {
                        Log.d(TAG, "Client ID conflict, đang reset và thử lại")
                        resetClientId() // Reset Client ID

                        // Thử kết nối lại sau 1 giây
                        Handler(Looper.getMainLooper()).postDelayed({
                            connect()
                        }, 1000)
                        return@launch
                    }
                }

                // Thông báo lỗi kết nối
                withContext(Dispatchers.Main) {
                    mqttCallback?.onConnectionFailed(e)
                }
            }
        }
    }

    // =================================
    // PUBLISH METHODS - Các phương thức gửi message
    // =================================

    /**
     * Gửi message tới topic cụ thể
     * @param topic Topic để gửi
     * @param message Nội dung message
     * @param retained Có lưu message trên broker không
     */
    fun publish(topic: String, message: String, retained: Boolean = true) {
        coroutineScope.launch { // Launch trong coroutine
            try {
                // Kiểm tra kết nối trước khi gửi
                if (!isConnected || mqttClient?.isConnected != true) {
                    Log.w(TAG, "Không thể gửi tin nhắn - Chưa kết nối, đang thử kết nối lại")
                    connect() // Thử kết nối lại
                    delay(2000) // Đợi kết nối

                    if (!isConnected) {
                        Log.e(TAG, "Không thể kết nối để gửi message")
                        return@launch
                    }
                }

                // Gửi message với QoS 1 và retained flag
                mqttClient?.publish(topic, message.toByteArray(), 1, retained)
                Log.d(TAG, "✅ Gửi thành công: $topic = $message (retained: $retained)")

            } catch (e: MqttException) {
                Log.e(TAG, "❌ Lỗi gửi message tới $topic: ${e.message}", e)

                // Nếu mất kết nối, thử kết nối lại
                if (!isConnected) {
                    connect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi không xác định khi gửi message: ${e.message}", e)
            }
        }
    }

    // =================================
    // PUMP CONTROL METHODS - Điều khiển máy bơm
    // =================================

    /**
     * Điều khiển trạng thái máy bơm (bật/tắt)
     * @param isOn true = bật máy bơm, false = tắt máy bơm
     */
    fun setStatusPump(isOn: Boolean) {
        val value = if (isOn) "1" else "0"
        publish(Topics.STATUS_PUMP, value, true)
        Log.i(TAG, "🚰 Điều khiển máy bơm: ${if (isOn) "BẬT" else "TẮT"}")
    }

    /**
     * Điều khiển chế độ tự động máy bơm
     * @param enabled true = bật chế độ tự động, false = chế độ thủ công
     */
    fun setAutoMode(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        publish(Topics.MODE_AUTO, value, true)
        Log.i(TAG, "🤖 Chế độ máy bơm: ${if (enabled) "TỰ ĐỘNG" else "THỦ CÔNG"}")
    }

    /**
     * Thiết lập ngưỡng độ ẩm đất tối đa cho chế độ tự động
     * @param threshold Ngưỡng độ ẩm tối đa (0-100%)
     */
    fun setHumidityLandMax(threshold: Int) {
        if (threshold in 1..100) {
            publish(Topics.HUMIDITY_LAND_MAX, threshold.toString(), true)
            Log.i(TAG, "📊 Ngưỡng độ ẩm MAX: $threshold%")
        } else {
            Log.w(TAG, "⚠️ Ngưỡng độ ẩm MAX không hợp lệ: $threshold (phải từ 1-100)")
        }
    }

    /**
     * Thiết lập ngưỡng độ ẩm đất tối thiểu cho chế độ tự động
     * @param threshold Ngưỡng độ ẩm tối thiểu (0-100%)
     */
    fun setHumidityLandMin(threshold: Int) {
        if (threshold in 0..99) {
            publish(Topics.HUMIDITY_LAND_MIN, threshold.toString(), true)
            Log.i(TAG, "📊 Ngưỡng độ ẩm MIN: $threshold%")
        } else {
            Log.w(TAG, "⚠️ Ngưỡng độ ẩm MIN không hợp lệ: $threshold (phải từ 0-99)")
        }
    }

    // =================================
    // BLIND CONTROL METHODS - Điều khiển rèm cửa
    // =================================

    /**
     * Điều khiển trạng thái rèm cửa (mở/đóng)
     * @param isOpen true = mở rèm, false = đóng rèm
     */
    fun setStatusBlind(isOpen: Boolean) {
        val value = if (isOpen) "1" else "0"
        publish(Topics.STATUS_BLIND, value, true)
        Log.i(TAG, "🪟 Điều khiển rèm cửa: ${if (isOpen) "MỞ" else "ĐÓNG"}")
    }

    // =================================
    // LEGACY METHODS - Tương thích với code cũ
    // =================================

    /**
     * Điều khiển tưới nước - tương thích với code cũ
     */
    fun setStatusWatering(isOn: Boolean) {
        setStatusPump(isOn) // Delegate tới method mới
    }

    /**
     * Chế độ tự động tưới - tương thích với code cũ
     */
    fun setAutoWatering(enabled: Boolean) {
        setAutoMode(enabled) // Delegate tới method mới
    }

    /**
     * Ngưỡng độ ẩm max - tương thích với code cũ
     */
    fun setHumidityLandMaxWatering(threshold: Int) {
        setHumidityLandMax(threshold) // Delegate tới method mới
    }

    /**
     * [LEGACY] Ngưỡng độ ẩm min - tương thích với code cũ
     */
    fun setHumidityLandMinWatering(threshold: Int) {
        setHumidityLandMin(threshold) // Delegate tới method mới
    }

    /**
     * [LEGACY] Điều khiển rèm tự động - hiện tại không có trên ESP32
     */
    fun setAutoBlind(enabled: Boolean) {
        Log.w(TAG, "⚠️ setAutoBlind() không được hỗ trợ bởi ESP32 hiện tại")
        // Không gửi command vì ESP32 chưa implement auto blind
    }

    /**
     * [LEGACY] Thiết lập ngưỡng mưa cho rèm - hiện tại không có trên ESP32
     */
    fun setRainValueBlind(threshold: Int) {
        Log.w(TAG, "⚠️ setRainValueBlind() không được hỗ trợ bởi ESP32 hiện tại")
        // Không gửi command vì ESP32 chưa implement rain threshold
    }

    // =================================
    // CONNECTION MANAGEMENT - Quản lý kết nối
    // =================================

    /**
     * Ngắt kết nối MQTT
     */
    fun disconnect() {
        coroutineScope.launch {
            try {
                if (mqttClient?.isConnected == true) {
                    mqttClient?.disconnect(0) // Ngắt kết nối ngay lập tức
                    Log.d(TAG, "Ngắt kết nối MQTT thành công")
                }
                isConnected = false

                // Thông báo đã ngắt kết nối
                withContext(Dispatchers.Main) {
                    mqttCallback?.onDisconnected()
                }
            } catch (e: MqttException) {
                Log.e(TAG, "Lỗi khi ngắt kết nối MQTT", e)
            }
        }
    }

    /**
     * Kiểm tra trạng thái kết nối
     */
    fun isConnected(): Boolean = isConnected && (mqttClient?.isConnected == true)

    /**
     * Cleanup resources khi không còn sử dụng
     */
    fun cleanup() {
        disconnect() // Ngắt kết nối
        coroutineScope.cancel() // Hủy coroutine scope
        Log.d(TAG, "Cleanup MQTT Handler hoàn tất")
    }

    // =================================
    // UTILITY METHODS - Các phương thức tiện ích
    // =================================
}