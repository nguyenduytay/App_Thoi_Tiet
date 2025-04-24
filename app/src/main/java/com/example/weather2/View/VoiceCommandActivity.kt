package com.example.weather2.View

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.weather2.Model.Fribase.FirebaseWatering
import com.example.weather2.Model.Fribase.FirebaseWeatherData
import com.example.weather2.Model.Fribase.FirebaseWindowBlinds
import java.util.*

class VoiceCommandActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false

    private val recognitionIntent: Intent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói lệnh của bạn...")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra quyền
        if (checkPermissions()) {
            // Khởi tạo TTS
            textToSpeech = TextToSpeech(this, this)

            // Hiện Toast thông báo
            Toast.makeText(this, "Hãy nói lệnh của bạn...", Toast.LENGTH_SHORT).show()

            // Phát âm thanh để cho biết đang lắng nghe
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi phát âm thanh: ${e.message}")
            }

            // Bắt đầu nhận diện giọng nói ngay lập tức
            setupSpeechRecognition()
        } else {
            // Nếu không có quyền, kết thúc Activity
            finish()
        }
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
            return false
        }
        return true
    }

    private fun setupSpeechRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Bắt đầu lắng nghe
                }

                override fun onBeginningOfSpeech() {
                    // Người dùng bắt đầu nói
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Âm lượng thay đổi
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Nhận buffer
                }

                override fun onEndOfSpeech() {
                    // Kết thúc nói
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Lỗi âm thanh"
                        SpeechRecognizer.ERROR_CLIENT -> "Lỗi client"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Thiếu quyền"
                        SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Hết thời gian mạng"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận diện được"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Đang bận"
                        SpeechRecognizer.ERROR_SERVER -> "Lỗi máy chủ"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Hết thời gian nói"
                        else -> "Lỗi không xác định"
                    }
                    Toast.makeText(this@VoiceCommandActivity, errorMessage, Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        Toast.makeText(
                            this@VoiceCommandActivity,
                            "Đã nhận diện: $recognizedText",
                            Toast.LENGTH_SHORT
                        ).show()
                        processCommand(recognizedText)
                    } else {
                        finish()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Kết quả tạm thời
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Sự kiện khác
                }
            })

            // Bắt đầu lắng nghe ngay lập tức
            speechRecognizer.startListening(recognitionIntent)
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    private fun processCommand(command: String) {
        val keyWorkStart = listOf("mở", "bật", "kích hoạt", "khởi động", "chạy", "vận hành", "phát động", "bắt đầu", "lên", "kéo", "kéo lên", "mở lên", "cho chạy", "on", "bật lên")
        val keyWorkEnd = listOf("đóng", "tắt", "ngắt", "dừng", "vô hiệu hóa", "ngừng", "tạm dừng", "ngắt nguồn", "kết thúc", "xuống", "cho nghỉ", "tắt đi", "off", "ngắt máy", "nghỉ", "dừng lại")
        val targetBom = listOf("máy bơm", "bơm", "máy bớm", "bơm nước")
        val targetManChe = listOf("màn", "màn che", "rèm", "màn chắn", "dàn phơi", "màn phơi", "dàn che")

        val keyAskWeather = listOf("thời tiết", "trời hôm nay", "ngoài trời như nào", "trời hiện tại", "thời tiết hôm nay", "trời ra sao", "dự báo")
        val keyAskTemp = listOf("nhiệt độ", "trời nóng không", "trời lạnh không", "nhiệt độ bao nhiêu", "nhiệt độ hiện tại")
        val keyAskHumidity = listOf("độ ẩm không khí", "độ ẩm", "không khí ẩm không", "không khí có ẩm không", "độ ẩm bao nhiêu")
        val keyAskSoilHumidity = listOf("độ ẩm đất", "đất có ẩm không", "ẩm đất", "độ ẩm đất hiện tại", "đất khô không")

        val commandLower = command.lowercase()

        // --------- Điều khiển thiết bị ----------
        when {
            keyWorkStart.any { commandLower.contains(it) } && targetBom.any { commandLower.contains(it) } && commandLower.contains("tự động") -> {
                val content = "Đã ${keyWorkStart.find { commandLower.contains(it) }} ${targetBom.find { commandLower.contains(it) }} tự động"
                FirebaseWatering.setWateringStatusHumidityLand(1)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            keyWorkEnd.any { commandLower.contains(it) } && targetBom.any { commandLower.contains(it) } && commandLower.contains("tự động") -> {
                val content = "Đã ${keyWorkEnd.find { commandLower.contains(it) }} ${targetBom.find { commandLower.contains(it) }} tự động"
                FirebaseWatering.setWateringStatusHumidityLand(0)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            keyWorkStart.any { commandLower.contains(it) } && targetBom.any { commandLower.contains(it) } -> {
                val content = "Đã ${keyWorkStart.find { commandLower.contains(it) }} ${targetBom.find { commandLower.contains(it) }}"
                FirebaseWatering.setWateringStatus(1)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            keyWorkEnd.any { commandLower.contains(it) } && targetBom.any { commandLower.contains(it) } -> {
                val content = "Đã ${keyWorkEnd.find { commandLower.contains(it) }} ${targetBom.find { commandLower.contains(it) }}"
                FirebaseWatering.setWateringStatus(0)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            keyWorkStart.any { commandLower.contains(it) } && targetManChe.any { commandLower.contains(it) } && commandLower.contains("tự động") -> {
                val content = "Đã ${keyWorkStart.find { commandLower.contains(it) }} ${targetManChe.find { commandLower.contains(it) }} tự động"
                FirebaseWindowBlinds.setWindowBlindsStatusAutomatic(1)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            keyWorkEnd.any { commandLower.contains(it) } && targetManChe.any { commandLower.contains(it) } && commandLower.contains("tự động") -> {
                val content = "Đã ${keyWorkEnd.find { commandLower.contains(it) }} ${targetManChe.find { commandLower.contains(it) }} tự động"
                FirebaseWindowBlinds.setWindowBlindsStatusAutomatic(0)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            keyWorkStart.any { commandLower.contains(it) } && targetManChe.any { commandLower.contains(it) } -> {
                val content = "Đã ${keyWorkStart.find { commandLower.contains(it) }} ${targetManChe.find { commandLower.contains(it) }}"
                FirebaseWindowBlinds.setWindowBlindsStatus(1)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            keyWorkEnd.any { commandLower.contains(it) } && targetManChe.any { commandLower.contains(it) } -> {
                val content = "Đã ${keyWorkEnd.find { commandLower.contains(it) }} ${targetManChe.find { commandLower.contains(it) }}"
                FirebaseWindowBlinds.setWindowBlindsStatus(0)
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }
        }

        // --------- Xử lý câu hỏi thời tiết ----------
        val askWeather = keyAskWeather.any { commandLower.contains(it) }
        val askTemp = keyAskTemp.any { commandLower.contains(it) }
        val askHumidity = keyAskHumidity.any { commandLower.contains(it) }
        val askSoilHumidity = keyAskSoilHumidity.any { commandLower.contains(it) }

        // Nếu có bất kỳ câu hỏi về thời tiết nào
        if (askWeather || askTemp || askHumidity || askSoilHumidity) {
            val weather = FirebaseWeatherData.getWeatherData()

            // Trường hợp 1: Hỏi về thời tiết tổng quát - trả lời tất cả
            if (askWeather) {
                val temp = weather.temperature?.toInt() ?: 0
                val tempDesc = when {
                    temp > 35 -> "rất nóng"
                    temp in 28..35 -> "nóng"
                    temp in 20..28 -> "mát mẻ"
                    else -> "lạnh"
                }

                val humidity = weather.humidity?.toInt() ?: 0
                val humidityDesc = when {
                    humidity > 80 -> "rất ẩm"
                    humidity in 60..80 -> "ẩm"
                    humidity in 40..59 -> "khá khô"
                    else -> "khô"
                }

                val humidityLand = weather.humidityLand?.toInt() ?: 0
                val soilDesc = when {
                    humidityLand > 80 -> "rất ướt"
                    humidityLand in 60..80 -> "ẩm"
                    humidityLand in 40..59 -> "hơi khô"
                    else -> "khá khô, cần tưới thêm"
                }

                val content = "Thời tiết hiện tại: nhiệt độ ${temp}°, trời $tempDesc; " +
                        "độ ẩm không khí ${humidity}%, trời $humidityDesc; " +
                        "độ ẩm đất ${humidityLand}%, đất $soilDesc."

                Toast.makeText(this, content, Toast.LENGTH_LONG).show()
                speakResponse(content)
                return
            }

            // Trường hợp 2: Chỉ hỏi về nhiệt độ
            else if (askTemp) {
                val temp = weather.temperature?.toInt() ?: 0
                val tempDesc = when {
                    temp > 35 -> "rất nóng"
                    temp in 28..35 -> "nóng"
                    temp in 20..28 -> "mát mẻ"
                    else -> "lạnh"
                }

                val content = "Nhiệt độ hiện tại là ${temp}°, trời $tempDesc."
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            // Trường hợp 3: Chỉ hỏi về độ ẩm không khí
            else if (askHumidity) {
                val humidity = weather.humidity?.toInt() ?: 0
                val humidityDesc = when {
                    humidity > 80 -> "rất ẩm"
                    humidity in 60..80 -> "ẩm"
                    humidity in 40..59 -> "khá khô"
                    else -> "khô"
                }

                val content = "Độ ẩm không khí hiện tại là ${humidity}%, trời $humidityDesc."
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }

            // Trường hợp 4: Chỉ hỏi về độ ẩm đất
            else if (askSoilHumidity) {
                val humidityLand = weather.humidityLand?.toInt() ?: 0
                val soilDesc = when {
                    humidityLand > 80 -> "rất ướt"
                    humidityLand in 60..80 -> "ẩm"
                    humidityLand in 40..59 -> "hơi khô"
                    else -> "khá khô, cần tưới thêm"
                }

                val content = "Độ ẩm đất hiện tại là ${humidityLand}%, đất $soilDesc."
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                speakResponse(content)
                return
            }
        }

        // --------- Không hiểu lệnh ----------
        Toast.makeText(this, "Tôi không hiểu lệnh của bạn", Toast.LENGTH_SHORT).show()
        speakResponse("Tôi không hiểu lệnh của bạn")
    }


    private fun speakResponse(response: String) {
        if (::textToSpeech.isInitialized && isTtsReady) {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "response_id")

            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, params, "response_id")

            // Đặt timeout để đóng activity sau khi nói xong
            textToSpeech.setOnUtteranceProgressListener(object :
                android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Đang nói
                }

                override fun onDone(utteranceId: String?) {
                    // Đã nói xong, đóng activity
                    finish()
                }

                override fun onError(utteranceId: String?) {
                    // Lỗi, đóng activity
                    finish()
                }
            })
        } else {
            Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
            // Đóng activity sau 2 giây nếu TTS không hoạt động
            window.decorView.postDelayed({ finish() }, 2000)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US)
            }
            textToSpeech.setPitch(1.0f)
            textToSpeech.setSpeechRate(0.9f)
            isTtsReady = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Khởi tạo TTS và bắt đầu nhận diện giọng nói
                textToSpeech = TextToSpeech(this, this)
                setupSpeechRecognition()
            } else {
                Toast.makeText(
                    this,
                    "Cần quyền ghi âm để sử dụng tính năng này",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "VoiceCommandActivity"
        private const val RECORD_AUDIO_PERMISSION_CODE = 101
    }
}