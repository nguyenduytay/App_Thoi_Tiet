package com.example.weather2.View

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.weather2.Model.Entity.E_WateringFirebase
import com.example.weather2.Model.Entity.E_WeatherDataFirebase
import com.example.weather2.Mqtt.MqttHandler
import com.example.weather2.ViewModel.WeatherDataViewModel
import com.example.weather2.ViewModel.WateringViewModel
import com.example.weather2.databinding.ActivateSystemBinding
import com.example.weather2.databinding.FragmentSystemBinding
import com.example.weather2.databinding.ParameterSystemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Fragment quản lý giao diện hệ thống điều khiển tưới cây và rèm cửa tự động
 * Sử dụng MVVM pattern với ViewModels để quản lý dữ liệu
 */
class SystemFragment : Fragment(), MqttHandler.MqttCallback {

    // =================================
    // BINDING PROPERTIES - View binding cho các layout
    // =================================
    private var _binding: FragmentSystemBinding? = null // Nullable binding để tránh memory leak
    private val binding get() = _binding!! // Property delegate để truy cập binding an toàn

    private lateinit var bindingParameterSystem: ParameterSystemBinding // Binding cho parameter layout
    private lateinit var bindingActivateSystem: ActivateSystemBinding // Binding cho activate layout

    // =================================
    // VIEWMODELS - Quản lý dữ liệu theo MVVM pattern
    // =================================
    private lateinit var weatherDataViewModel: WeatherDataViewModel // ViewModel cho dữ liệu thời tiết
    private lateinit var wateringViewModel: WateringViewModel // ViewModel cho dữ liệu tưới nước

    // =================================
    // COROUTINE JOBS - Quản lý các coroutine
    // =================================
    private var timeUpdateJob: Job? = null // Job để cập nhật thời gian real-time
    private var blinkSecondJob: Job? = null // Job để tạo hiệu ứng nhấp nháy giây
    private var timerProgressUpdateJob: Job? = null // Job để cập nhật thanh tiến trình hẹn giờ

    // =================================
    // STATE MANAGEMENT - Quản lý trạng thái
    // =================================
    private var isUpdatingFromViewModel = false // Cờ để tránh vòng lặp vô hạn khi cập nhật UI

    // =================================
    // MQTT HANDLER - Xử lý giao tiếp MQTT
    // =================================
    private lateinit var mqttHandler: MqttHandler // Handler để giao tiếp với thiết bị IoT

    private var mqttCommandTime = 0L
    private val MQTT_DELAY = 1000L

    // ✅ Thêm các flag control
    private var isUIUpdating = false
    private var isListenersEnabled = true // ← QUAN TRỌNG: Control listeners

    // ✅ Debounce job cho UI updates
    private var uiUpdateJob: Job? = null
    private val UI_UPDATE_DELAY = 150L

    /**
     * Khởi tạo giao diện và thiết lập các thành phần
     * Được gọi khi fragment được tạo và hiển thị lên màn hình
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Khởi tạo binding cho layout chính
        _binding = FragmentSystemBinding.inflate(inflater, container, false)

        // Khởi tạo binding cho các include layout
        bindingParameterSystem =
            ParameterSystemBinding.bind(binding.includeParameterSystem.parameterSystem)
        bindingActivateSystem =
            ActivateSystemBinding.bind(binding.includeActivateSystem.activateSystem)

        // Khởi tạo các thành phần chính
        initViewModels() // Tạo instances của ViewModels
        initMqtt() // Khởi tạo MQTT handler
        setupObservers() // Thiết lập observers để lắng nghe dữ liệu
        setupUI() // Thiết lập UI components và interactions
        startUIUpdates() // Bắt đầu các coroutines cập nhật UI

        return binding.root // Trả về root view
    }

    /**
     * Khởi tạo các ViewModels sử dụng ViewModelProvider
     * ViewModelProvider đảm bảo ViewModels survive configuration changes
     */
    private fun initViewModels() {
        weatherDataViewModel = ViewModelProvider(this)[WeatherDataViewModel::class.java]
        wateringViewModel = ViewModelProvider(this)[WateringViewModel::class.java]
    }

    /**
     * Khởi tạo MQTT handler và thiết lập callback
     */
    private fun initMqtt() {
        mqttHandler = MqttHandler(requireContext()) // Tạo MQTT handler với context
        mqttHandler.setCallback(this) // Set callback để nhận thông báo từ MQTT
        mqttHandler.connect() // Kết nối tới MQTT broker
    }

    /**
     * Thiết lập tất cả observers để lắng nghe dữ liệu từ ViewModels
     */
    private fun setupObservers() {
        observeWeatherData() // Lắng nghe dữ liệu thời tiết
        observeWateringData() // Lắng nghe dữ liệu tưới nước
        observeErrors() // Lắng nghe và xử lý lỗi
        observeUpdateSuccess() // Lắng nghe thông báo cập nhật thành công
    }

    /**
     * Observer dữ liệu thời tiết từ WeatherDataViewModel
     */
    private fun observeWeatherData() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                weatherDataViewModel.currentWeatherData.collect { weatherData -> // Collect weather data
                    weatherData?.let { data -> // Nếu data không null
                        updateWeatherUI(data) // Cập nhật UI với dữ liệu thời tiết
                    }
                }
            }
        }
    }

    /**
     * Observer dữ liệu tưới nước từ WateringViewModel
     */
    private fun observeWateringData() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                wateringViewModel.wateringData.collect { wateringData -> // Collect watering data
                    wateringData?.let { data -> // Nếu data không null
                        updateWateringUI(data) // Cập nhật UI với dữ liệu tưới nước
                    }
                }
            }
        }
    }

    /**
     * Observer lỗi từ ViewModels
     */
    private fun observeErrors() {
        // Observer lỗi từ WeatherDataViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                weatherDataViewModel.error.collect { error ->
                    error?.let { // Nếu có lỗi
                        showError("Lỗi dữ liệu thời tiết: $it") // Hiển thị lỗi
                        weatherDataViewModel.clearError() // Clear lỗi sau khi hiển thị
                    }
                }
            }
        }

        // Observer lỗi từ WateringViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wateringViewModel.error.collect { error ->
                    error?.let { // Nếu có lỗi
                        showError("Lỗi hệ thống tưới: $it") // Hiển thị lỗi
                        wateringViewModel.clearError() // Clear lỗi sau khi hiển thị
                    }
                }
            }
        }
    }

    /**
     * Observer thông báo cập nhật thành công
     */
    private fun observeUpdateSuccess() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wateringViewModel.updateSuccess.collect { success ->
                    if (success) { // Nếu cập nhật thành công
                        // Có thể hiển thị thông báo thành công
                        wateringViewModel.clearUpdateSuccess() // Clear flag sau khi xử lý
                    }
                }
            }
        }
    }

    /**
     * Thiết lập UI components và interactions
     */
    private fun setupUI() {
        setupSpinner() // Thiết lập spinner chọn ngày
        setupTimeTimer() // Thiết lập chức năng hẹn giờ
        setupWateringControls() // Thiết lập điều khiển tưới nước
        setupWindowBlindsControls() // Thiết lập điều khiển rèm cửa
    }

    /**
     * Bắt đầu các coroutines cập nhật UI
     */
    private fun startUIUpdates() {
        startTimeUpdates() // Bắt đầu cập nhật thời gian
        startBlinkAnimation() // Bắt đầu hiệu ứng nhấp nháy
        startTimerProgressUpdates() // Bắt đầu cập nhật thanh tiến trình
    }

    // =================================
    // UI UPDATE METHODS - Các hàm cập nhật UI
    // =================================

    /**
     * Cập nhật UI hiển thị dữ liệu thời tiết
     */
    @SuppressLint("SetTextI18n") // Suppress warning về hardcoded string
    private fun updateWeatherUI(weatherData: E_WeatherDataFirebase) {
        try {
            // Cập nhật TextView hiển thị nhiệt độ với null safety
            bindingParameterSystem.tvTempParameterSystem.text =
                weatherData.temperature.toString().plus(" ℃") ?: "N/A"

            // Cập nhật TextView hiển thị độ ẩm không khí với null safety
            bindingParameterSystem.tvHumidityAirParameterSystem.text =
                weatherData.humidity.toString().plus(" %") ?: "N/A"

            // Cập nhật TextView hiển thị độ ẩm đất với null safety
            bindingParameterSystem.tvHumidityLandParameterSystem.text =
                weatherData.humidityLand?.toString()?.plus(" %") ?: "N/A"

            // Cập nhật SeekBar nhiệt độ (adjust range để hiển thị đúng)
            bindingParameterSystem.sbTempParameterSystem.progress =
                (weatherData.temperature.roundToInt().plus(25)) ?: 0

            // Cập nhật SeekBar độ ẩm không khí
            bindingParameterSystem.sbHumidityAirParameterSystem.progress =
                weatherData.humidity.roundToInt() ?: 0

            // Cập nhật SeekBar độ ẩm đất
            bindingParameterSystem.sbHumidityLandParameterSystem.progress =
                weatherData.humidityLand.roundToInt() ?: 0


            //thiết lập mưa, áp suất, nắng
            bindingActivateSystem.tvRain.text =if(weatherData.rain > 3000) "Không mưa" else "Mưa"
            bindingActivateSystem.tvPressure.text = (weatherData.pressure).toInt().toString() + " hPa"
            bindingActivateSystem.tvSun.text = if((weatherData.light > 3500)) "Ánh sáng yếu" else if(weatherData.light > 1500) "Ánh sáng vừa" else "Ánh sáng mạnh"

            isUpdatingFromViewModel = true

            bindingActivateSystem.swWaterSystem.isChecked = weatherData.status_pump
            bindingActivateSystem.swWindowBlindsSystem.isChecked = weatherData.status_blind
            bindingActivateSystem.swAutomaticSystem.isChecked = weatherData.auto_mode

            bindingActivateSystem.npHumidityMinLandSystem.isEnabled = weatherData.auto_mode
            bindingActivateSystem.npHumidityMaxLandSystem.isEnabled = weatherData.auto_mode
            bindingActivateSystem.btUpdateHumidityLand.isEnabled = weatherData.auto_mode

            isUpdatingFromViewModel = false


        } catch (e: Exception) {
            // Log lỗi và hiển thị thông báo cho user
            Log.e("SystemFragment", "Error updating weather UI", e)
            showError("Lỗi cập nhật giao diện thời tiết: ${e.message}")
        }
    }

    /**
     * Cập nhật UI hiển thị dữ liệu tưới nước
     */
    private fun updateWateringUI(wateringData: E_WateringFirebase) {
        isUpdatingFromViewModel = true // Set flag để tránh vòng lặp
        try {

            val defaultMin = wateringData.humidity_land_min ?: 20
            val defaultMax = wateringData.humidity_land_max ?: 80

            // Setup NumberPicker Min
            bindingActivateSystem.npHumidityMinLandSystem.apply {
                minValue = 0
                maxValue = 99
                setFormatter { value -> "  $value % " }
                value = defaultMin

                setOnValueChangedListener { _, _, newMin ->
                    // Tự động cập nhật Max NumberPicker
                    bindingActivateSystem.npHumidityMaxLandSystem.apply {
                        minValue = newMin + 1
                        if (value <= newMin) value = newMin + 1
                    }
                }
            }

            // Setup NumberPicker Max
            bindingActivateSystem.npHumidityMaxLandSystem.apply {
                minValue = 1
                maxValue = 100
                setFormatter { value -> "  $value % " }
                value = defaultMax

                setOnValueChangedListener { _, _, newMax ->
                    // Tự động cập nhật Min NumberPicker
                    bindingActivateSystem.npHumidityMinLandSystem.apply {
                        maxValue = newMax - 1
                        if (value >= newMax) value = newMax - 1
                    }
                }
            }

            // Cập nhật constraints lần đầu
            bindingActivateSystem.npHumidityMinLandSystem.maxValue = defaultMax - 1
            bindingActivateSystem.npHumidityMaxLandSystem.minValue = defaultMin + 1

            //chế độ tự động tưới
            bindingActivateSystem.btUpdateHumidityLand.setOnClickListener {
                val humidityLandMin = bindingActivateSystem.npHumidityMinLandSystem.value
                val humidityLandMax = bindingActivateSystem.npHumidityMaxLandSystem.value
                setHumidityLandMinWatering(humidityLandMin)
                setHumidityLandMaxWatering(humidityLandMax)
                wateringViewModel.updateField("humidity_land_max", humidityLandMax)
                wateringViewModel.updateField("humidity_land_min", humidityLandMin)
                Toast.makeText(
                    context,
                    "Đã thiết lập mức độ ẩm đất ",
                    Toast.LENGTH_SHORT
                ).show()
            }


            // Cập nhật switch hẹn giờ và enable/disable các controls liên quan
            val isTimerEnabled = wateringData.status_timer == 1
            bindingActivateSystem.swTimeTimerSystem.isChecked = isTimerEnabled
            bindingActivateSystem.btTimerStartSystem.isEnabled = isTimerEnabled
            bindingActivateSystem.btTimerEndSystem.isEnabled = isTimerEnabled
            bindingActivateSystem.btSelectTimerSystem.isEnabled = isTimerEnabled

            // Cập nhật hiển thị thời gian bắt đầu và kết thúc
            val startTimeString = getTimeString(wateringData.timer_start)
            bindingActivateSystem.tvTimerStartSystem.text = startTimeString
            bindingParameterSystem.tvTimerParameterSystem.text = startTimeString
            bindingActivateSystem.tvTimerEndSystem.text = getTimeString(wateringData.timer_end)

            // Cập nhật hiển thị các ngày lặp lại
            bindingActivateSystem.tvSelectDayTimerSystem.text = wateringData.repeat ?: ""

        } catch (e: Exception) {
            Log.e("SystemFragment", "Error updating watering UI", e)
            showError("Lỗi cập nhật giao diện tưới nước: ${e.message}")
        } finally {
            isUpdatingFromViewModel = false // Reset flag sau khi cập nhật xong
        }
    }

    /**
     * Hiển thị thông báo lỗi cho user
     */
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        Log.e("SystemFragment", message)
    }

    // =================================
    // TIME UPDATE METHODS - Các hàm cập nhật thời gian
    // =================================

    /**
     * Bắt đầu coroutine để cập nhật thời gian real-time
     */
    private fun startTimeUpdates() {
        timeUpdateJob =
            viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
                repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ chạy khi Fragment active
                    while (isActive) { // Lặp vô hạn trong khi coroutine active
                        val currentTime = getCurrentTime() // Lấy thời gian hiện tại
                        val currentDayMonth = getCurrentDayMonthEndYear() // Lấy ngày tháng năm

                        withContext(Dispatchers.Main) { // Switch to main thread để cập nhật UI
                            // Cập nhật các TextView hiển thị thời gian
                            binding.tvTimeSystem.text = currentTime.first // Hiển thị giờ:phút
                            binding.tvTimeFormatSystem.text = currentTime.second // Hiển thị AM/PM
                            binding.tvRankDaySystem.text =
                                getCurrentDayInVN() // Hiển thị thứ bằng tiếng Việt
                            binding.tvYearSystem.text = currentDayMonth.second // Hiển thị năm
                            binding.tvDayMonthSystem.text =
                                currentDayMonth.first // Hiển thị ngày tháng
                        }
                        delay(1000) // Delay 1 giây trước khi cập nhật tiếp
                    }
                }
            }
    }

    /**
     * Bắt đầu hiệu ứng nhấp nháy cho các chấm giây
     */
    private fun startBlinkAnimation() {
        // Danh sách các TextView hiển thị chấm giây
        val blinkSecondTextList: List<TextView> = listOf(
            binding.tvTimeSecond1System,
            binding.tvTimeSecond2System,
            binding.tvTimeSecond3System,
            binding.tvTimeSecond4System,
            binding.tvTimeSecond5System,
            binding.tvTimeSecond6System
        )

        blinkSecondJob = viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ chạy khi Fragment active
                while (isActive) { // Lặp vô hạn
                    // Sáng dần từ chấm 1 đến chấm 6
                    for (i in blinkSecondTextList.indices) {
                        withContext(Dispatchers.Main) { // Switch to main thread
                            blinkSecondTextList[i].backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor("#F04E5E")) // Đổi màu sáng
                        }
                        delay(100) // Delay giữa mỗi chấm
                    }

                    delay(300) // Dừng lại khi tất cả đã sáng

                    // Tắt dần từ chấm 6 về chấm 1
                    for (i in blinkSecondTextList.indices.reversed()) {
                        withContext(Dispatchers.Main) { // Switch to main thread
                            blinkSecondTextList[i].backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor("#D9D9D9")) // Đổi màu tắt
                        }
                        delay(100) // Delay giữa mỗi chấm
                    }

                    delay(300) // Dừng lại khi tất cả đã tắt
                }
            }
        }
    }

    // =================================
    // UTILITY TIME METHODS - Các hàm tiện ích về thời gian
    // =================================

    /**
     * Lấy thứ trong tuần bằng tiếng Việt
     */
    private fun getCurrentDayInVN(): String {
        val calendar = Calendar.getInstance() // Lấy Calendar instance
        val daysOfWeek = arrayOf( // Array tên các thứ bằng tiếng Việt
            "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
        )
        return daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1] // Return tên thứ tương ứng
    }

    /**
     * Lấy thứ hiện tại dưới dạng viết tắt
     */
    private fun getDayShort(): String {
        val calendar = Calendar.getInstance() // Lấy Calendar instance
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Lấy day of week

        return when (dayOfWeek) { // Map day of week thành viết tắt
            Calendar.MONDAY -> "T2"
            Calendar.TUESDAY -> "T3"
            Calendar.WEDNESDAY -> "T4"
            Calendar.THURSDAY -> "T5"
            Calendar.FRIDAY -> "T6"
            Calendar.SATURDAY -> "T7"
            Calendar.SUNDAY -> "CN"
            else -> ""
        }
    }

    /**
     * Lấy thời gian hiện tại định dạng 12h với AM/PM
     */
    private fun getCurrentTime(): Pair<String, String> {
        val sdfTime = SimpleDateFormat("hh : mm", Locale.getDefault()) // Formatter cho giờ:phút
        val sdfAmPm = SimpleDateFormat("a", Locale.getDefault()) // Formatter cho AM/PM

        val time = sdfTime.format(Date()) // Format thời gian
        val amPm = sdfAmPm.format(Date()) // Format AM/PM
        return Pair(time, amPm) // Return cặp thời gian và AM/PM
    }

    /**
     * Lấy ngày tháng năm hiện tại
     */
    private fun getCurrentDayMonthEndYear(): Pair<String, String> {
        val sdfDayMonth =
            SimpleDateFormat("dd ' - Tháng: ' MM", Locale("vi", "VN")) // Formatter ngày tháng
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault()) // Formatter năm

        val dayMonth = sdfDayMonth.format(Date()) // Format ngày tháng
        val year = sdfYear.format(Date()) // Format năm
        return Pair(dayMonth, year) // Return cặp ngày tháng và năm
    }

    /**
     * Chuyển đổi phút trong ngày thành string HH:mm
     */
    private fun getTimeString(time: Int?): String {
        if (time == null) return "00 : 00" // Return default nếu null

        val hours = time / 60 // Tính số giờ
        val minute = time % 60 // Tính số phút
        val houseString = if (hours < 10) ("0$hours") else ("$hours") // Format giờ với leading zero
        val minuteString =
            if (minute < 10) ("0$minute") else ("$minute") // Format phút với leading zero
        return ("$houseString : $minuteString") // Return formatted time
    }

    /**
     * Extract giờ và phút từ time string
     */
    private fun extractHourMinute(timeString: String): Pair<Int, Int>? {
        return try {
            val timeParts = timeString.split(":").map { it.trim() } // Split và trim whitespace
            val hour = timeParts[0].toInt() // Parse giờ
            val minute = timeParts[1].toInt() // Parse phút

            if (hour in 0..23 && minute in 0..59) { // Validate range
                Pair(hour, minute) // Return valid pair
            } else {
                null // Return null nếu invalid
            }
        } catch (e: Exception) {
            null // Return null nếu parse error
        }
    }

    // =================================
    // TIMER PROGRESS METHODS - Các hàm xử lý thanh tiến trình hẹn giờ
    // =================================

    /**
     * Bắt đầu cập nhật thanh tiến trình hẹn giờ
     */
    private fun startTimerProgressUpdates() {
        timerProgressUpdateJob?.cancel() // Hủy job cũ nếu có

        timerProgressUpdateJob = viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine mới
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ chạy khi active
                while (isActive) { // Lặp vô hạn
                    updateProgressOnWateringTimer() // Cập nhật progress bar
                    delay(1000) // Cập nhật mỗi giây
                }
            }
        }
    }

    /**
     * Cập nhật thanh tiến trình cho hẹn giờ tưới nước
     */
    private fun updateProgressOnWateringTimer() {
        try {
            // Nếu timer không bật, set progress về 0
            if (!bindingActivateSystem.swTimeTimerSystem.isChecked) {
                bindingParameterSystem.sbTimerParameterSystem.progress = 0
                return
            }

            // Kiểm tra hôm nay có phải ngày tưới không
            val isSelectDay = isWateringTimerOfDay()
            if (!isSelectDay) {
                bindingParameterSystem.sbTimerParameterSystem.progress = 0
                return
            }

            // Lấy thời gian bắt đầu và kết thúc
            val startTimeText = bindingActivateSystem.tvTimerStartSystem.text.toString()
            val endTimeText = bindingActivateSystem.tvTimerEndSystem.text.toString()

            val startTime = extractHourMinute(startTimeText) // Parse start time
            val endTime = extractHourMinute(endTimeText) // Parse end time

            if (startTime == null || endTime == null) return // Return nếu parse lỗi

            // Chuyển đổi thành phút trong ngày
            val startTimeInMinutes = startTime.first * 60 + startTime.second
            val endTimeInMinutes = endTime.first * 60 + endTime.second

            // Thời gian hiện tại tính theo phút và giây
            val calendar = Calendar.getInstance()
            val currentTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 +
                    calendar.get(Calendar.MINUTE) +
                    calendar.get(Calendar.SECOND) / 60.0

            // Tính progress dựa vào thời gian hiện tại
            val progress = when {
                currentTimeInMinutes < startTimeInMinutes -> 100 // Chưa tới giờ bắt đầu
                currentTimeInMinutes >= endTimeInMinutes -> 0 // Hết giờ tưới
                else -> { // Đang trong khoảng tưới
                    val totalTimeRange = endTimeInMinutes - startTimeInMinutes
                    val currentTimeRange = currentTimeInMinutes - startTimeInMinutes
                    val progressValue = 100 - ((currentTimeRange / totalTimeRange) * 100).toInt()
                    progressValue.coerceIn(0, 100) // Clamp trong range 0-100
                }
            }

            bindingParameterSystem.sbTimerParameterSystem.progress = progress // Set progress

        } catch (e: Exception) {
            Log.e("SystemFragment", "Error updating timer progress", e)
        }
    }

    /**
     * Kiểm tra ngày hiện tại có trong danh sách ngày được chọn không
     */
    private fun isWateringTimerOfDay(): Boolean {
        val selectDaysText = bindingActivateSystem.tvSelectDayTimerSystem.text.toString()

        // Nếu chọn "Mỗi ngày" thì return true
        if (selectDaysText == "Mỗi ngày") {
            return true
        }

        val currentDayShort = getDayShort() // Lấy thứ hiện tại dạng viết tắt
        return selectDaysText.contains(currentDayShort) // Check có trong list không
    }

    // =================================
    // UI SETUP METHODS - Các hàm thiết lập UI
    // =================================

    /**
     * Thiết lập spinner chọn ngày trong tuần
     */
    private fun setupSpinner() {
        val items = arrayOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật")
        val shortItems = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dayOrderMap =
            mapOf("T2" to 1, "T3" to 2, "T4" to 3, "T5" to 4, "T6" to 5, "T7" to 6, "CN" to 7)
        val selectItems = BooleanArray(items.size) // Array để track selected items
        val selectList = mutableListOf<String>() // List chứa các item đã chọn

        // Set click listener cho button chọn ngày
        bindingActivateSystem.btSelectTimerSystem.setOnClickListener {
            AlertDialog.Builder(requireContext()).apply { // Tạo AlertDialog
                setTitle("Chọn các mục") // Set title cho dialog
                setMultiChoiceItems(
                    items,
                    selectItems
                ) { _, which, isChecked -> // Set multi choice items
                    selectItems[which] = isChecked // Update selected state
                    if (isChecked) { // Nếu được chọn
                        if (!selectList.contains(shortItems[which])) { // Nếu chưa có trong list
                            selectList.add(shortItems[which]) // Thêm vào list
                        }
                    } else { // Nếu bỏ chọn
                        selectList.remove(shortItems[which]) // Remove khỏi list
                    }
                }
                setPositiveButton("OK") { _, _ -> // Set positive button
                    // Sort list theo thứ tự trong tuần
                    val sortedList = selectList.sortedBy { dayOrderMap[it] ?: Int.MAX_VALUE }
                    // Tạo text hiển thị
                    val selectedText =
                        if (selectList.size == 7) "Mỗi ngày" else sortedList.joinToString(", ")
                    bindingActivateSystem.tvSelectDayTimerSystem.text = selectedText // Update UI

                    // Cập nhật ViewModel nếu timer đã bật
                    if (bindingActivateSystem.swTimeTimerSystem.isChecked && !isUpdatingFromViewModel) {
                        wateringViewModel.updateField("repeat", selectedText)
                    }
                }
                setNegativeButton("Hủy", null) // Set negative button
            }.show() // Show dialog
        }
    }

    /**
     * Thiết lập chức năng chọn thời gian hẹn giờ
     */
    @SuppressLint("SimpleDateFormat") // Suppress warning về SimpleDateFormat
    private fun setupTimeTimer() {
        var startTime: Calendar? = null // Biến lưu thời gian bắt đầu
        var endTime: Calendar? = null // Biến lưu thời gian kết thúc

        // Thiết lập button chọn thời gian bắt đầu
        bindingActivateSystem.btTimerStartSystem.setOnClickListener {
            val cal = Calendar.getInstance() // Lấy Calendar instance
            val timeSetListener =
                TimePickerDialog.OnTimeSetListener { _, hour, minute -> // Listener khi chọn time
                    cal.set(Calendar.HOUR_OF_DAY, hour) // Set giờ
                    cal.set(Calendar.MINUTE, minute) // Set phút
                    startTime = cal.clone() as Calendar // Clone calendar
                    val timeFormatted = SimpleDateFormat("HH : mm").format(cal.time) // Format time
                    bindingActivateSystem.tvTimerStartSystem.text = timeFormatted // Update UI

                    // Cập nhật ViewModel nếu timer đã bật
                    if (bindingActivateSystem.swTimeTimerSystem.isChecked && !isUpdatingFromViewModel) {
                        wateringViewModel.updateField("timer_start", hour * 60 + minute)
                    }
                }
            // Show TimePickerDialog
            TimePickerDialog(
                requireContext(),
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true // Use 24h format
            ).show()
        }

        // Thiết lập button chọn thời gian kết thúc
        bindingActivateSystem.btTimerEndSystem.setOnClickListener {
            val cal = Calendar.getInstance() // Lấy Calendar instance
            val timeSetListener =
                TimePickerDialog.OnTimeSetListener { _, hour, minute -> // Listener khi chọn time
                    cal.set(Calendar.HOUR_OF_DAY, hour) // Set giờ
                    cal.set(Calendar.MINUTE, minute) // Set phút

                    // Validation: check đã chọn start time chưa
                    if (startTime == null) {
                        Toast.makeText(
                            requireContext(),
                            "Vui lòng chọn thời gian bắt đầu trước!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnTimeSetListener
                    }

                    // Validation: check end time > start time
                    if (cal.timeInMillis <= startTime!!.timeInMillis) {
                        Toast.makeText(
                            requireContext(),
                            "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnTimeSetListener
                    }

                    endTime = cal.clone() as Calendar // Clone calendar
                    val timeFormatted = SimpleDateFormat("HH : mm").format(cal.time) // Format time
                    bindingActivateSystem.tvTimerEndSystem.text = timeFormatted // Update UI

                    // Cập nhật ViewModel nếu timer đã bật
                    if (bindingActivateSystem.swTimeTimerSystem.isChecked && !isUpdatingFromViewModel) {
                        wateringViewModel.updateField("timer_end", hour * 60 + minute)
                    }
                }
            // Show TimePickerDialog
            TimePickerDialog(
                requireContext(),
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true // Use 24h format
            ).show()
        }
    }

    /**
     * Thiết lập các điều khiển tưới nước
     */
    private fun setupWateringControls() {
        // Set click listener cho chế độ tự động
        bindingActivateSystem.swAutomaticSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromViewModel) {
                setAutoWatering(checked)
            }

            if (checked) {
                //tắt chế độ điều khiển màn che
                bindingActivateSystem.swWindowBlindsSystem.isChecked = false
                bindingActivateSystem.swWindowBlindsSystem.isClickable = false
                bindingActivateSystem.swWindowBlindsSystem.isFocusable = false
                //tắt chế độ điều khiển máy bơm
                bindingActivateSystem.swWaterSystem.isChecked = false
                bindingActivateSystem.swWaterSystem.isClickable = false
                bindingActivateSystem.swWaterSystem.isFocusable = false
                // tắt chế độ hẹn giờ tưới nước nếu bật
                bindingActivateSystem.swTimeTimerSystem.isChecked = false
                bindingActivateSystem.swTimeTimerSystem.isClickable = false
                bindingActivateSystem.swTimeTimerSystem.isFocusable = false
                wateringViewModel.updateField("status_timer", 0)

            } else {
                //bật lại chế độ điều khiển thủ công
                bindingActivateSystem.swWindowBlindsSystem.isClickable = true
                bindingActivateSystem.swWindowBlindsSystem.isFocusable = true
                //bạt lại chế độ điêu fkhirrnt máy bơm thủ công
                bindingActivateSystem.swWaterSystem.isClickable = true
                bindingActivateSystem.swWaterSystem.isFocusable = true
                // bật lại chế độ hẹn giờ tưới nước
                bindingActivateSystem.swTimeTimerSystem.isClickable = true
                bindingActivateSystem.swTimeTimerSystem.isFocusable = true

            }
        }

        // Set click listener cho switch tưới nước thủ công
        bindingActivateSystem.swWaterSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                if (checked) openWatering() else closeWatering() // Send MQTT command
            }
        }

        // Set click listener cho switch hẹn giờ
        bindingActivateSystem.swTimeTimerSystem.setOnClickListener  {
            val checked = bindingActivateSystem.swTimeTimerSystem.isChecked
            if (!isUpdatingFromViewModel ) { // Chỉ update nếu không phải từ ViewModel

                wateringViewModel.updateField("status_timer", if (checked) 1 else 0)
                // Enable/disable related controls
                bindingActivateSystem.btTimerStartSystem.isEnabled = checked
                bindingActivateSystem.btTimerEndSystem.isEnabled = checked
                bindingActivateSystem.btSelectTimerSystem.isEnabled = checked

                // Update related fields nếu bật timer
                if (checked) {
                    val timeTextStart = bindingActivateSystem.tvTimerStartSystem.text.toString()
                    val timeStart = extractHourMinute(timeTextStart)

                    val timeTextEnd = bindingActivateSystem.tvTimerEndSystem.text.toString()
                    val timeEnd = extractHourMinute(timeTextEnd)

                    val repeat = bindingActivateSystem.tvSelectDayTimerSystem.text.toString()

                    // Update các field nếu có data
                    timeStart?.let {
                        wateringViewModel.updateField(
                            "timer_start",
                            it.first * 60 + it.second
                        )
                    }
                    timeEnd?.let {
                        wateringViewModel.updateField(
                            "timer_end",
                            it.first * 60 + it.second
                        )
                    }
                    if (repeat.isNotEmpty()) {
                        wateringViewModel.updateField("repeat", repeat)
                    }
                }
            }
        }

        // Set listeners cho TextViews để sync với ViewModel khi thay đổi
        bindingActivateSystem.tvTimerStartSystem.doAfterTextChanged { text ->
            if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                val newTimeStart = extractHourMinute(text.toString())
                if (newTimeStart != null && bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    wateringViewModel.updateField(
                        "timer_start",
                        newTimeStart.first * 60 + newTimeStart.second
                    )
                }
            }
        }

        bindingActivateSystem.tvTimerEndSystem.doAfterTextChanged { text ->
            if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                val newTimeEnd = extractHourMinute(text.toString())
                if (newTimeEnd != null && bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    wateringViewModel.updateField(
                        "timer_end",
                        newTimeEnd.first * 60 + newTimeEnd.second
                    )
                }
            }
        }

        bindingActivateSystem.tvSelectDayTimerSystem.doAfterTextChanged { text ->
            if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                val newRepeat = text.toString()
                if (bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    wateringViewModel.updateField("repeat", newRepeat)
                }
            }
        }
    }

    /**
     * Thiết lập các điều khiển rèm cửa
     */
    @SuppressLint("SetTextI18n") // Suppress warning về hardcoded string
    private fun setupWindowBlindsControls() {
        // Set listener cho switch rèm cửa thủ công
        bindingActivateSystem.swWindowBlindsSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromViewModel) {
                if (checked) openBlind() else closeBlind() // Send MQTT command
            }
        }

    }

    // =================================
    // MQTT COMMAND METHODS - Các hàm gửi lệnh MQTT
    // =================================

    /**
     * Mở rèm cửa thủ công
     */
    private fun openBlind() {
        val now = System.currentTimeMillis()
        if (now - mqttCommandTime < MQTT_DELAY) {
            Log.d("SystemFragment", "MQTT too fast, skipped")
            return
        }
        mqttCommandTime = now
        if (mqttHandler.isConnected()) { // Check MQTT connection
            mqttHandler.setStatusBlind(true) // Send command
            Toast.makeText(context, "Đã mở màn che", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Đóng rèm cửa thủ công
     */
    private fun closeBlind() {
        val now = System.currentTimeMillis()
        if (now - mqttCommandTime < MQTT_DELAY) {
            Log.d("SystemFragment", "MQTT too fast, skipped")
            return
        }
        mqttCommandTime = now

        if (mqttHandler.isConnected()) { // Check MQTT connection
            mqttHandler.setStatusBlind(false) // Send command
            Toast.makeText(context, "Đã đóng màn che", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAutoWatering(enabled: Boolean) {
        val now = System.currentTimeMillis()
        if (now - mqttCommandTime < MQTT_DELAY) {
            Log.d("SystemFragment", "MQTT too fast, skipped")
            return
        }
        mqttCommandTime = now

        if (mqttHandler.isConnected()) {
            mqttHandler.setAutoWatering(enabled)
            val mode = if (enabled) "tự động" else "thủ công"
            Toast.makeText(context, "Chế độ: $mode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWatering() {
        val now = System.currentTimeMillis()
        if (now - mqttCommandTime < MQTT_DELAY) return
        mqttCommandTime = now

        if (mqttHandler.isConnected()) {
            mqttHandler.setStatusWatering(true)
            Toast.makeText(context, "Bật máy bơm", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeWatering() {
        val now = System.currentTimeMillis()
        if (now - mqttCommandTime < MQTT_DELAY) return
        mqttCommandTime = now

        if (mqttHandler.isConnected()) {
            mqttHandler.setStatusWatering(false)
            Toast.makeText(context, "Tắt máy bơm", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Thiết lập độ ẩm đất tối đa
     */
    private fun setHumidityLandMaxWatering(value: Int) {
        if (mqttHandler.isConnected()) { // Check MQTT connection
            mqttHandler.setHumidityLandMaxWatering(value) // Send command
        } else {
            Toast.makeText(context, "Chưa kết nối MQTT", Toast.LENGTH_SHORT).show()
            mqttHandler.connect() // Try reconnect
        }
    }

    /**
     * Thiết lập độ ẩm đất tối thiểu
     */
    private fun setHumidityLandMinWatering(value: Int) {
        if (mqttHandler.isConnected()) { // Check MQTT connection
            mqttHandler.setHumidityLandMinWatering(value) // Send command
        } else {
            Toast.makeText(context, "Chưa kết nối MQTT", Toast.LENGTH_SHORT).show()
            mqttHandler.connect() // Try reconnect
        }
    }

    // =================================
    // MQTT CALLBACK METHODS - Các callback từ MQTT
    // =================================

    /**
     * Callback khi MQTT kết nối thành công
     */
    override fun onConnected() {
        Log.d("MQTT", "Kết nối MQTT thành công") // Log success
        activity?.runOnUiThread { // Switch to main thread
            Toast.makeText(context, "Đã kết nối MQTT thành công", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Callback khi MQTT mất kết nối
     */
    override fun onDisconnected() {
        Log.w("MQTT", "Mất kết nối MQTT") // Log disconnection
        activity?.runOnUiThread { // Switch to main thread
            Toast.makeText(context, "Mất kết nối MQTT", Toast.LENGTH_SHORT).show()
            mqttHandler.connect() // Try reconnect
        }
    }

    override fun onMessageReceived(topic: String, message: String) {

    }

    /**
     * Callback khi MQTT kết nối thất bại
     */
    override fun onConnectionFailed(exception: Throwable?) {
        Log.e("MQTT", "Kết nối MQTT thất bại", exception) // Log error
        activity?.runOnUiThread { // Switch to main thread
            Toast.makeText(
                context,
                "Kết nối MQTT thất bại: ${exception?.message}",
                Toast.LENGTH_SHORT
            ).show()
            // Thử kết nối lại sau 5 giây
            Handler(Looper.getMainLooper()).postDelayed({
                mqttHandler.connect()
            }, 5000)
        }
    }

    // =================================
    // LIFECYCLE METHODS - Quản lý lifecycle
    // =================================

    /**
     * Called khi view bị destroy
     * Cleanup resources để tránh memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()

        // Cleanup MQTT
        mqttHandler.disconnect() // Ngắt kết nối MQTT
        mqttHandler.cleanup() // Cleanup MQTT resources

        // Cancel tất cả coroutine jobs
        timeUpdateJob?.cancel() // Cancel time update job
        blinkSecondJob?.cancel() // Cancel blink animation job
        timerProgressUpdateJob?.cancel() // Cancel timer progress job

        // Set binding null để tránh memory leak
        _binding = null
    }
}