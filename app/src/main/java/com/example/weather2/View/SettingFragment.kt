package com.example.weather2.View

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.weather2.Model.Entity.E_NotificationConfigFirebase
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.example.weather2.Notification.FCMTokenManager
import com.example.weather2.ViewModel.NotificationConfigViewModel
import com.example.weather2.ViewModel.WarningConfigViewModel
import com.example.weather2.databinding.FragmentSettingBinding
import com.example.weather2.databinding.NotificationSettingBinding
import com.example.weather2.databinding.WarningSettingBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment quản lý cài đặt thông báo và cảnh báo
 * Sử dụng MVVM pattern với ViewModels để quản lý dữ liệu
 */
class SettingFragment : Fragment() {

    // =================================
    // BINDING PROPERTIES - View binding cho các layout
    // =================================
    private var _binding: FragmentSettingBinding? = null // Nullable binding để tránh memory leak
    private val binding get() = _binding!! // Property delegate để truy cập binding an toàn

    private lateinit var bindingNotificationSetting: NotificationSettingBinding // Binding cho notification layout
    private lateinit var bindingWarningSetting: WarningSettingBinding // Binding cho warning layout

    // =================================
    // VIEWMODELS - Quản lý dữ liệu theo MVVM pattern
    // =================================
    private lateinit var notificationViewModel: NotificationConfigViewModel // ViewModel cho notification config
    private lateinit var warningViewModel: WarningConfigViewModel // ViewModel cho warning config

    // =================================
    // STATE MANAGEMENT - Quản lý trạng thái
    // =================================
    private var isUpdatingFromViewModel = false // Cờ để tránh vòng lặp vô hạn khi cập nhật UI

    /**
     * Khởi tạo giao diện Fragment và thiết lập các thành phần
     * Được gọi khi fragment được tạo và hiển thị lên màn hình
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Khởi tạo binding cho layout chính
        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        // Khởi tạo binding cho các include layout
        bindingNotificationSetting =
            NotificationSettingBinding.bind(binding.includeNotificationSetting.notificationSetting)
        bindingWarningSetting =
            WarningSettingBinding.bind(binding.includeWarningSetting.warningSetting)

        // Khởi tạo các thành phần chính
        initViewModels() // Tạo instances của ViewModels
        setupObservers() // Thiết lập observers để lắng nghe dữ liệu
        setupUI() // Thiết lập UI components và interactions

        return binding.root // Trả về root view
    }

    /**
     * Khởi tạo các ViewModels sử dụng ViewModelProvider
     * ViewModelProvider đảm bảo ViewModels survive configuration changes
     */
    private fun initViewModels() {
        notificationViewModel = ViewModelProvider(this)[NotificationConfigViewModel::class.java]
        warningViewModel = ViewModelProvider(this)[WarningConfigViewModel::class.java]
    }

    /**
     * Thiết lập tất cả observers để lắng nghe dữ liệu từ ViewModels
     */
    private fun setupObservers() {
        observeNotificationConfig() // Lắng nghe notification config
        observeWarningConfig() // Lắng nghe warning config
        observeLoadingStates() // Lắng nghe trạng thái loading
        observeErrors() // Lắng nghe và xử lý lỗi
        observeUpdateSuccess() // Lắng nghe thông báo cập nhật thành công
    }

    /**
     * Observer notification config từ NotificationConfigViewModel
     */
    private fun observeNotificationConfig() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                notificationViewModel.notificationConfig.collect { config -> // Collect notification config
                    config?.let { data -> // Nếu data không null
                        updateNotificationUI(data) // Cập nhật UI với notification config
                    }
                }
            }
        }
    }

    /**
     * Observer warning config từ WarningConfigViewModel
     */
    private fun observeWarningConfig() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                warningViewModel.warningConfig.collect { config -> // Collect warning config
                    config?.let { data -> // Nếu data không null
                        updateWarningUI(data) // Cập nhật UI với warning config
                    }
                }
            }
        }
    }

    /**
     * Observer trạng thái loading từ ViewModels
     */
    private fun observeLoadingStates() {
        // Observer loading từ NotificationViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationViewModel.isLoading.collect { isLoading ->
                    updateLoadingState(isLoading, "notification") // Update loading cho notification section
                }
            }
        }

        // Observer loading từ WarningViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                warningViewModel.isLoading.collect { isLoading ->
                    updateLoadingState(isLoading, "warning") // Update loading cho warning section
                }
            }
        }
    }

    /**
     * Observer lỗi từ ViewModels
     */
    private fun observeErrors() {
        // Observer lỗi từ NotificationViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationViewModel.error.collect { error ->
                    error?.let { // Nếu có lỗi
                        showError("Lỗi cấu hình thông báo: $it") // Hiển thị lỗi
                        notificationViewModel.clearError() // Clear lỗi sau khi hiển thị
                    }
                }
            }
        }

        // Observer lỗi từ WarningViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                warningViewModel.error.collect { error ->
                    error?.let { // Nếu có lỗi
                        showError("Lỗi cấu hình cảnh báo: $it") // Hiển thị lỗi
                        warningViewModel.clearError() // Clear lỗi sau khi hiển thị
                    }
                }
            }
        }
    }

    /**
     * Observer thông báo cập nhật thành công
     */
    private fun observeUpdateSuccess() {
        // Observer success từ NotificationViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationViewModel.updateSuccess.collect { success ->
                    if (success) { // Nếu cập nhật thành công
                        showSuccess("Cập nhật thông báo thành công") // Hiển thị thông báo thành công
                        notificationViewModel.clearUpdateSuccess() // Clear flag
                    }
                }
            }
        }

        // Observer success từ WarningViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                warningViewModel.updateSuccess.collect { success ->
                    if (success) { // Nếu cập nhật thành công
                        showSuccess("Cập nhật cảnh báo thành công") // Hiển thị thông báo thành công
                        warningViewModel.clearUpdateSuccess() // Clear flag
                    }
                }
            }
        }
    }

    /**
     * Thiết lập UI components và interactions
     */
    private fun setupUI() {
        setupNumberPickers() // Thiết lập NumberPickers
        setupNotificationControls() // Thiết lập điều khiển thông báo
        setupWarningControls() // Thiết lập điều khiển cảnh báo
        setupTimePickerDialog() // Thiết lập dialog chọn thời gian
    }

    // =================================
    // UI UPDATE METHODS - Các hàm cập nhật UI
    // =================================

    /**
     * Cập nhật UI hiển thị notification config
     */
    private fun updateNotificationUI(config: E_NotificationConfigFirebase) {
        isUpdatingFromViewModel = true // Set flag để tránh vòng lặp
        try {
            // Cập nhật switch chính
            bindingNotificationSetting.swNotificationSetting.isChecked = config.status

            // Cập nhật các checkbox
            bindingNotificationSetting.cbTempNotificationSetting.isChecked = config.temp
            bindingNotificationSetting.cbHumidityAirNotificationSetting.isChecked = config.humidityAir
            bindingNotificationSetting.cbHumidityLandNotificationSetting.isChecked = config.humidityLand

            // Cập nhật hiển thị thời gian
            bindingNotificationSetting.tvTimeNotificationSetting.text = getTimeString(config.time)

            // Cập nhật trạng thái enable/disable của các controls
            val listNotification = listOf(
                bindingNotificationSetting.ibTimeNotificationSetting,
                bindingNotificationSetting.cbTempNotificationSetting,
                bindingNotificationSetting.cbHumidityAirNotificationSetting,
                bindingNotificationSetting.cbHumidityLandNotificationSetting
            )
            listNotification.forEach { it.isEnabled = config.status } // Enable/disable based on switch

        } catch (e: Exception) {
            showError("Lỗi cập nhật giao diện thông báo: ${e.message}")
        } finally {
            isUpdatingFromViewModel = false // Reset flag
        }
    }

    /**
     * Cập nhật UI hiển thị warning config
     */
    private fun updateWarningUI(config: E_WarningConfigFirebase) {
        isUpdatingFromViewModel = true // Set flag để tránh vòng lặp
        try {
            // Cập nhật switch chính
            bindingWarningSetting.swWarningSetting.isChecked = config.status == 1

            // Cập nhật NumberPickers với giá trị từ config
            bindingWarningSetting.npTempMaxWarningSetting.value = config.tempMax
            bindingWarningSetting.npTempMinWarningSetting.value = config.tempMin
            bindingWarningSetting.npHumidityAirMaxWarningSetting.value = config.humidityAirMax
            bindingWarningSetting.npHumidityAirMinWarningSetting.value = config.humidityAirMin
            bindingWarningSetting.npHumidityLandMaxWarningSetting.value = config.humidityLandMax
            bindingWarningSetting.npHumidityLandMinWarningSetting.value = config.humidityLandMin

            // Cập nhật checkboxes với trạng thái từ config
            bindingWarningSetting.cbTempMaxWarningSetting.isChecked = config.tempStatusMax == 1
            bindingWarningSetting.cbTempMinWarningSetting.isChecked = config.tempStatusMin == 1
            bindingWarningSetting.cbHumidityAirMaxWarningSetting.isChecked = config.humidityAirStatusMax == 1
            bindingWarningSetting.cbHumidityAirMinWarningSetting.isChecked = config.humidityAirStatusMin == 1
            bindingWarningSetting.cbHumidityLandMaxWarningSetting.isChecked = config.humidityLandStatusMax == 1
            bindingWarningSetting.cbHumidityLandMinWarningSetting.isChecked = config.humidityLandStatusMin == 1

            // Cập nhật trạng thái enable/disable của các controls
            val listWarningCheckBox = listOf(
                bindingWarningSetting.cbTempMaxWarningSetting,
                bindingWarningSetting.cbTempMinWarningSetting,
                bindingWarningSetting.cbHumidityAirMaxWarningSetting,
                bindingWarningSetting.cbHumidityAirMinWarningSetting,
                bindingWarningSetting.cbHumidityLandMaxWarningSetting,
                bindingWarningSetting.cbHumidityLandMinWarningSetting
            )
            val listWarningNumberPicker = listOf(
                bindingWarningSetting.npTempMaxWarningSetting,
                bindingWarningSetting.npTempMinWarningSetting,
                bindingWarningSetting.npHumidityAirMaxWarningSetting,
                bindingWarningSetting.npHumidityAirMinWarningSetting,
                bindingWarningSetting.npHumidityLandMaxWarningSetting,
                bindingWarningSetting.npHumidityLandMinWarningSetting
            )

            val isEnabled = config.status == 1
            listWarningCheckBox.forEach { it.isEnabled = isEnabled } // Enable/disable checkboxes
            listWarningNumberPicker.forEach { it.isEnabled = isEnabled } // Enable/disable NumberPickers

        } catch (e: Exception) {
            showError("Lỗi cập nhật giao diện cảnh báo: ${e.message}")
        } finally {
            isUpdatingFromViewModel = false // Reset flag
        }
    }

    /**
     * Cập nhật trạng thái loading
     */
    private fun updateLoadingState(isLoading: Boolean, section: String) {
        // Có thể disable/enable controls hoặc hiển thị progress indicator
        val alpha = if (isLoading) 0.5f else 1.0f
        when (section) {
            "notification" -> bindingNotificationSetting.root.alpha = alpha
            "warning" -> bindingWarningSetting.root.alpha = alpha
        }
    }

    /**
     * Hiển thị thông báo lỗi cho user
     */
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Hiển thị thông báo thành công cho user
     */
    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // =================================
    // UI SETUP METHODS - Các hàm thiết lập UI
    // =================================

    /**
     * Thiết lập các NumberPickers với giá trị min, max và formatter
     */
    private fun setupNumberPickers() {
        // NumberPickers cho độ ẩm (hiển thị %)
        val humidityNumberPickers = listOf(
            bindingWarningSetting.npHumidityAirMaxWarningSetting,
            bindingWarningSetting.npHumidityAirMinWarningSetting,
            bindingWarningSetting.npHumidityLandMaxWarningSetting,
            bindingWarningSetting.npHumidityLandMinWarningSetting
        )

        // NumberPickers cho nhiệt độ (hiển thị ℃)
        val temperatureNumberPickers = listOf(
            bindingWarningSetting.npTempMaxWarningSetting,
            bindingWarningSetting.npTempMinWarningSetting
        )

        // Cấu hình NumberPickers cho độ ẩm
        humidityNumberPickers.forEach { numberPicker ->
            numberPicker.apply {
                maxValue = 100 // Max 100%
                minValue = 0 // Min 0%
                setFormatter { value -> "  $value % " } // Format với %
                value = 50 // Default value
                isEnabled = false // Initially disabled
            }
        }

        // Cấu hình NumberPickers cho nhiệt độ
        temperatureNumberPickers.forEach { numberPicker ->
            numberPicker.apply {
                val min = -100 // Min temperature
                val max = 100 // Max temperature
                minValue = 0 // Start index from 0
                maxValue = max - min // Total range
                val values = (min..max).map { "$it ℃" }.toTypedArray() // Create display values array
                displayedValues = values // Set display values
                value = 125 // Default to 25℃ (index 125 = -100 + 125 = 25)
                isEnabled = false // Initially disabled
            }
        }
    }

    /**
     * Thiết lập các điều khiển notification
     */
    private fun setupNotificationControls() {
        // Danh sách các controls notification
        val notificationControls = listOf(
            bindingNotificationSetting.ibTimeNotificationSetting,
            bindingNotificationSetting.cbTempNotificationSetting,
            bindingNotificationSetting.cbHumidityAirNotificationSetting,
            bindingNotificationSetting.cbHumidityLandNotificationSetting
        )

        // Set listener cho switch notification chính
        bindingNotificationSetting.swNotificationSetting.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                // Enable/disable tất cả controls
                notificationControls.forEach { it.isEnabled = checked }

                // Nếu tắt notification, uncheck tất cả checkboxes
                if (!checked) {
                    notificationControls.takeLast(3).forEach { control ->
                        if (control is CheckBox) {
                            control.isChecked = false
                        }
                    }
                }

                updateNotificationConfig() // Update ViewModel
            }
        }

        // Set listeners cho các checkboxes
        notificationControls.takeLast(3).forEach { control ->
            if (control is CheckBox) {
                control.setOnClickListener {
                    if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                        updateNotificationConfig() // Update ViewModel
                    }
                }
            }
        }
    }

    /**
     * Thiết lập các điều khiển warning
     */
    private fun setupWarningControls() {
        // Danh sách các checkboxes warning
        val warningCheckBoxes = listOf(
            bindingWarningSetting.cbTempMaxWarningSetting,
            bindingWarningSetting.cbTempMinWarningSetting,
            bindingWarningSetting.cbHumidityAirMaxWarningSetting,
            bindingWarningSetting.cbHumidityAirMinWarningSetting,
            bindingWarningSetting.cbHumidityLandMaxWarningSetting,
            bindingWarningSetting.cbHumidityLandMinWarningSetting
        )

        // Danh sách các NumberPickers warning
        val warningNumberPickers = listOf(
            bindingWarningSetting.npTempMaxWarningSetting,
            bindingWarningSetting.npTempMinWarningSetting,
            bindingWarningSetting.npHumidityAirMaxWarningSetting,
            bindingWarningSetting.npHumidityAirMinWarningSetting,
            bindingWarningSetting.npHumidityLandMaxWarningSetting,
            bindingWarningSetting.npHumidityLandMinWarningSetting
        )

        // Set listener cho switch warning chính
        bindingWarningSetting.swWarningSetting.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                // Enable/disable tất cả controls
                warningCheckBoxes.forEach { it.isEnabled = checked }
                warningNumberPickers.forEach { it.isEnabled = checked }

                // Nếu tắt warning, uncheck tất cả checkboxes
                if (!checked) {
                    warningCheckBoxes.forEach { it.isChecked = false }
                }

                updateWarningConfig() // Update ViewModel
            }
        }

        // Set listeners cho các checkboxes
        warningCheckBoxes.forEach { checkBox ->
            checkBox.setOnClickListener {
                if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                    updateWarningConfig() // Update ViewModel
                }
            }
        }

        // Set listeners cho các NumberPickers
        warningNumberPickers.forEach { numberPicker ->
            numberPicker.setOnValueChangedListener { _, _, _ ->
                if (!isUpdatingFromViewModel) { // Chỉ update nếu không phải từ ViewModel
                    updateWarningConfig() // Update ViewModel
                }
            }
        }
    }

    /**
     * Thiết lập dialog chọn thời gian
     */
    @SuppressLint("SetTextI18n") // Suppress warning về hardcoded string
    private fun setupTimePickerDialog() {
        bindingNotificationSetting.ibTimeNotificationSetting.setOnClickListener {
            val hour = 0 // Default hour
            val minute = 0 // Default minute
            val is24HourView = true // Use 24-hour format

            // Tạo TimePickerDialog với custom theme
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                android.R.style.Theme_Holo_Light_Dialog, // Custom theme
                { _, selectedHour, selectedMinute -> // Callback khi chọn time
                    // Format hour và minute với leading zero
                    val hourString = if (selectedHour < 10) "0$selectedHour" else "$selectedHour"
                    val minuteString = if (selectedMinute < 10) "0$selectedMinute" else "$selectedMinute"

                    // Update UI
                    bindingNotificationSetting.tvTimeNotificationSetting.text = "$hourString : $minuteString"

                    // Update ViewModel nếu không phải đang update từ ViewModel
                    if (!isUpdatingFromViewModel) {
                        updateNotificationConfig()
                    }

                    // Show confirmation toast
                    Toast.makeText(requireContext(), "Đã chọn: $selectedHour:$selectedMinute", Toast.LENGTH_SHORT).show()
                },
                hour, minute, is24HourView
            )

            // Set transparent background và title
            timePickerDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            timePickerDialog.setTitle("Chọn thời gian")
            timePickerDialog.show() // Show dialog
        }
    }

    // =================================
    // UTILITY METHODS - Các hàm tiện ích
    // =================================

    /**
     * Chuyển đổi phút trong ngày thành string HH:mm
     */
    private fun getTimeString(time: Int): String {
        val hours = time / 60 // Tính số giờ
        val minutes = time % 60 // Tính số phút
        val hourString = if (hours < 10) "0$hours" else "$hours" // Format với leading zero
        val minuteString = if (minutes < 10) "0$minutes" else "$minutes" // Format với leading zero
        return "$hourString : $minuteString" // Return formatted time
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

    /**
     * Lấy thời gian hiện tại theo format HH:mm dd/MM
     */
    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date())
    }

    // =================================
    // VIEWMODEL UPDATE METHODS - Các hàm cập nhật ViewModel
    // =================================

    /**
     * Cập nhật notification config qua ViewModel
     */
    private fun updateNotificationConfig() {
        // Extract time từ UI
        val time = extractHourMinute(bindingNotificationSetting.tvTimeNotificationSetting.text.toString())

        // Tạo notification config object
        val notification = E_NotificationConfigFirebase(
            status = bindingNotificationSetting.swNotificationSetting.isChecked,
            time = (time?.first ?: 0) * 60 + (time?.second ?: 0), // Convert to minutes
            temp = bindingNotificationSetting.cbTempNotificationSetting.isChecked,
            humidityAir = bindingNotificationSetting.cbHumidityAirNotificationSetting.isChecked,
            humidityLand = bindingNotificationSetting.cbHumidityLandNotificationSetting.isChecked
        )

        // Update qua ViewModel
        notificationViewModel.updateNotificationConfig(notification)
    }

    /**
     * Cập nhật warning config qua ViewModel
     */
    private fun updateWarningConfig() {
        // Convert checkbox states to int (1 = checked, 0 = unchecked)
        val status = if (bindingWarningSetting.swWarningSetting.isChecked) 1 else 0
        val tempStatusMax = if (bindingWarningSetting.cbTempMaxWarningSetting.isChecked) 1 else 0
        val tempStatusMin = if (bindingWarningSetting.cbTempMinWarningSetting.isChecked) 1 else 0
        val humidityAirStatusMax = if (bindingWarningSetting.cbHumidityAirMaxWarningSetting.isChecked) 1 else 0
        val humidityAirStatusMin = if (bindingWarningSetting.cbHumidityAirMinWarningSetting.isChecked) 1 else 0
        val humidityLandStatusMax = if (bindingWarningSetting.cbHumidityLandMaxWarningSetting.isChecked) 1 else 0
        val humidityLandStatusMin = if (bindingWarningSetting.cbHumidityLandMinWarningSetting.isChecked) 1 else 0

        // Lấy FCM token để gửi thông báo
        FCMTokenManager.getToken { fcmToken ->
            // Tạo warning config object
            val warningConfig = E_WarningConfigFirebase(
                fcmToken = fcmToken ?: "", // Use empty string if token is null
                status = status,
                tempStatusMax = tempStatusMax,
                tempMax = bindingWarningSetting.npTempMaxWarningSetting.value,
                tempStatusMin = tempStatusMin,
                tempMin = bindingWarningSetting.npTempMinWarningSetting.value,
                humidityAirStatusMax = humidityAirStatusMax,
                humidityAirMax = bindingWarningSetting.npHumidityAirMaxWarningSetting.value,
                humidityAirStatusMin = humidityAirStatusMin,
                humidityAirMin = bindingWarningSetting.npHumidityAirMinWarningSetting.value,
                humidityLandStatusMax = humidityLandStatusMax,
                humidityLandMax = bindingWarningSetting.npHumidityLandMaxWarningSetting.value,
                humidityLandStatusMin = humidityLandStatusMin,
                humidityLandMin = bindingWarningSetting.npHumidityLandMinWarningSetting.value
            )

            // Update qua ViewModel
            warningViewModel.updateWarningConfig(warningConfig)
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
        _binding = null // Set binding null để tránh memory leak
    }
}