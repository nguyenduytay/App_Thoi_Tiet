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
import androidx.lifecycle.lifecycleScope
import com.example.weather2.Model.Entity.E_NotificationConfigFirebase
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.example.weather2.Model.Fribase.FirebaseNotificationConfig
import com.example.weather2.Model.Fribase.FirebaseWarningConfig
import com.example.weather2.View.Notification.FCMTokenManager
import com.example.weather2.databinding.FragmentSettingBinding
import com.example.weather2.databinding.NotificationSettingBinding
import com.example.weather2.databinding.WarningSettingBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragment quản lý cài đặt thông báo và cảnh báo
 * Cho phép người dùng cấu hình các ngưỡng cảnh báo và tùy chọn thông báo
 */
class SettingFragment : Fragment() {
    // Binding để truy cập các thành phần UI trong layout
    private lateinit var bindingSetting: FragmentSettingBinding
    private lateinit var bindingNotificationSetting: NotificationSettingBinding
    private lateinit var bindingWarningSetting: WarningSettingBinding

    // Biến cờ để kiểm soát việc cập nhật từ Firebase, tránh vòng lặp vô hạn
    private var isUpdatingFromFirebase = false

    // Lưu trữ các listener callback để có thể hủy khi cần, tránh memory leak
    private var warningListener: ((E_WarningConfigFirebase) -> Unit)? = null
    private var notificationListener: ((E_NotificationConfigFirebase) -> Unit)? = null

    /**
     * Khởi tạo giao diện Fragment và thiết lập các listener ban đầu
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Khởi tạo các binding để truy cập các thành phần UI
        bindingSetting = FragmentSettingBinding.inflate(inflater, container, false)
        bindingNotificationSetting =
            NotificationSettingBinding.bind(bindingSetting.includeNotificationSetting.notificationSetting)
        bindingWarningSetting =
            WarningSettingBinding.bind(bindingSetting.includeWarningSetting.warningSetting)

        // Thiết lập các NumberPicker với giá trị min, max phù hợp
        setNumberPicker()

        // Thay đổi thứ tự: Đầu tiên tải dữ liệu từ Firebase, sau đó mới thiết lập event listeners
        showNotificationFirebase()

        // Thiết lập event listeners sau khi đã tải dữ liệu
        onEndOff()  // Xử lý bật/tắt thông báo và cảnh báo
        openTimePickerDialog()  // Thiết lập hộp thoại chọn thời gian

        return bindingSetting.root
    }

    /**
     * Giải phóng tài nguyên khi Fragment bị hủy
     * Hủy các listener với Firebase để tránh memory leak
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Hủy đăng ký các listener với Firebase
        warningListener?.let { FirebaseWarningConfig.removeListener(it) }
        notificationListener?.let { FirebaseNotificationConfig.removeListener(it) }

        // Xóa tham chiếu để tránh memory leak
        warningListener = null
        notificationListener = null
    }

    /**
     * Thiết lập các NumberPicker với giá trị min, max và định dạng hiển thị
     * Có 2 loại: hiển thị phần trăm (%) và hiển thị nhiệt độ (℃)
     */
    private fun setNumberPicker() {
        // NumberPicker cho độ ẩm (hiển thị %)
        val list1 = listOf(
            bindingWarningSetting.npHumidityAirMaxWarningSetting,
            bindingWarningSetting.npHumidityAirMinWarningSetting,
            bindingWarningSetting.npHumidityLandMaxWarningSetting,
            bindingWarningSetting.npHumidityLandMinWarningSetting
        )
        // NumberPicker cho nhiệt độ (hiển thị ℃)
        val list2 = listOf(
            bindingWarningSetting.npTempMaxWarningSetting,
            bindingWarningSetting.npTempMinWarningSetting
        )

        // Cấu hình cho các NumberPicker độ ẩm
        for (i in list1) {
            i.maxValue = 100
            i.minValue = 0
            i.setFormatter { value -> "  $value % " }
            i.value = 89
            i.isEnabled = false
        }

        // Cấu hình cho các NumberPicker nhiệt độ
        for (i in list2) {
            val min = -100
            val max = 100
            i.minValue = 0
            i.maxValue = max - min
            val values = (min..max).map { "$it ℃" }.toTypedArray()
            i.displayedValues = values
            i.value = 125
            i.isEnabled = false
        }
    }

    /**
     * Thiết lập sự kiện bật/tắt thông báo và cảnh báo
     * Xử lý trạng thái kích hoạt của các control tùy thuộc vào trạng thái của switch
     */
    private fun onEndOff() {
        // Danh sách các control thông báo
        val listNotification = listOf(
            bindingNotificationSetting.ibTimeNotificationSetting,
            bindingNotificationSetting.cbTempNotificationSetting,
            bindingNotificationSetting.cbHumidityAirNotificationSetting,
            bindingNotificationSetting.cbHumidityLandNotificationSetting
        )

        // Danh sách các checkbox cảnh báo
        val listWarningCheckBox = listOf(
            bindingWarningSetting.cbTempMaxWarningSetting,
            bindingWarningSetting.cbTempMinWarningSetting,
            bindingWarningSetting.cbHumidityAirMaxWarningSetting,
            bindingWarningSetting.cbHumidityAirMinWarningSetting,
            bindingWarningSetting.cbHumidityLandMaxWarningSetting,
            bindingWarningSetting.cbHumidityLandMinWarningSetting
        )

        // Danh sách các NumberPicker cảnh báo
        val listWarningNumberPicker = listOf(
            bindingWarningSetting.npTempMaxWarningSetting,
            bindingWarningSetting.npTempMinWarningSetting,
            bindingWarningSetting.npHumidityAirMaxWarningSetting,
            bindingWarningSetting.npHumidityAirMinWarningSetting,
            bindingWarningSetting.npHumidityLandMaxWarningSetting,
            bindingWarningSetting.npHumidityLandMinWarningSetting
        )

        // Xử lý sự kiện thay đổi trạng thái thông báo
        bindingNotificationSetting.swNotificationSetting.setOnCheckedChangeListener { _, checked ->
            // Kiểm tra không phải đang cập nhật từ Firebase
            if (!isUpdatingFromFirebase) {
                // Bật/tắt tất cả các control thông báo
                listNotification.forEach { it.isEnabled = checked }
                // Nếu tắt thông báo, bỏ chọn tất cả các checkbox
                if(!checked) {
                    listNotification.takeLast(3).forEach {
                        if (it is CheckBox) {
                            it.isChecked = false
                        }
                    }
                }
                // Cập nhật trạng thái thông báo lên Firebase
                updateOrInsertNotification()
            }
        }

        // Thiết lập sự kiện cho các checkbox thông báo
        listNotification.takeLast(3).forEach {
            if (it is CheckBox) {
                it.setOnClickListener {
                    if (!isUpdatingFromFirebase) {
                        updateOrInsertNotification()
                    }
                }
            }
        }

        // Xử lý sự kiện thay đổi trạng thái cảnh báo
        bindingWarningSetting.swWarningSetting.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromFirebase) {
                // Bật/tắt tất cả các control cảnh báo
                listWarningCheckBox.forEach { it.isEnabled = checked }
                listWarningNumberPicker.forEach { it.isEnabled = checked }
                // Nếu tắt cảnh báo, bỏ chọn tất cả các checkbox
                if(!checked) {
                    for (i in listWarningCheckBox) {
                        i.isChecked=false
                    }
                }
                // Cập nhật trạng thái cảnh báo lên Firebase
                updateOrInsertWarning()
            }
        }

        // Thiết lập sự kiện cho các checkbox cảnh báo
        for (i in listWarningCheckBox) {
            i.setOnClickListener{
                if (!isUpdatingFromFirebase) {
                    updateOrInsertWarning()
                }
            }
        }

        // Thiết lập sự kiện cho các NumberPicker cảnh báo
        for(i in listWarningNumberPicker) {
            i.setOnValueChangedListener { _, _, _ ->
                if (!isUpdatingFromFirebase) {
                    updateOrInsertWarning()
                }
            }
        }
    }

    /**
     * Mở hộp thoại chọn thời gian để đặt lịch thông báo
     * Sử dụng TimePickerDialog với giao diện tùy chỉnh
     */
    @SuppressLint("SetTextI18n")
    private fun openTimePickerDialog() {
        bindingNotificationSetting.ibTimeNotificationSetting.setOnClickListener {
            val hour = 0
            val minute = 0
            val is24HourView = true

            val timePickerDialog = TimePickerDialog(
                requireContext(),
                android.R.style.Theme_Holo_Light_Dialog,
                { _, selectedHour, selectedMinute ->
                    // Định dạng giờ và phút có số 0 ở đầu nếu < 10
                    val hourString=if(selectedHour<10) "0$selectedHour" else "$selectedHour"
                    val minuteString=if(selectedMinute<10) "0$selectedMinute" else "$selectedMinute"
                    bindingNotificationSetting.tvTimeNotificationSetting.text = "$hourString : $minuteString"
                    // Cập nhật thông tin thông báo nếu không phải đang cập nhật từ Firebase
                    if (!isUpdatingFromFirebase) {
                        updateOrInsertNotification()
                    }
                    Toast.makeText(
                        requireContext(),
                        "Đã chọn: $selectedHour:$selectedMinute",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                hour,
                minute,
                is24HourView
            )
            // Đặt nền trong suốt cho hộp thoại
            timePickerDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Đặt tiêu đề
            timePickerDialog.setTitle("Chọn thời gian")
            // Hiển thị hộp thoại
            timePickerDialog.show()
        }
    }

    /**
     * Chuyển đổi thời gian từ số phút trong ngày thành chuỗi định dạng "HH : mm"
     * @param time Thời gian tính bằng phút từ đầu ngày
     * @return Chuỗi định dạng "HH : mm"
     */
    private fun getTimeString(time: Int): String {
        val house = time / 60
        val minute = time % 60
        val houseString = if (house < 10) ("0$house") else ("$house")
        val minuteString = if (minute < 10) ("0$minute") else ("$minute")
        return ("$houseString : $minuteString")
    }

    /**
     * Tách chuỗi thời gian thành giờ và phút
     * @param timeString Chuỗi thời gian định dạng "HH : mm"
     * @return Pair<Int, Int> chứa giờ và phút, hoặc null nếu không hợp lệ
     */
    private fun extractHourMinute(timeString: String): Pair<Int, Int>? {
        return try {
            val timeParts = timeString.split(":").map { it.trim() }
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            if (hour in 0..23 && minute in 0..59) {
                Pair(hour, minute)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Hiển thị thông tin cấu hình thông báo và cảnh báo từ Firebase
     * Đăng ký các listener để cập nhật UI khi dữ liệu thay đổi
     */
    private fun showNotificationFirebase() {
        // Khởi tạo listener cho cảnh báo
        warningListener = { warning ->
            isUpdatingFromFirebase = true
            try {
                // Cập nhật trạng thái của switch trước
                bindingWarningSetting.swWarningSetting.isChecked = warning.status == 1

                // Cập nhật trạng thái các NumberPicker
                bindingWarningSetting.npTempMaxWarningSetting.value = warning.tempMax
                bindingWarningSetting.npTempMinWarningSetting.value = warning.tempMin
                bindingWarningSetting.npHumidityAirMaxWarningSetting.value = warning.humidityAirMax
                bindingWarningSetting.npHumidityAirMinWarningSetting.value = warning.humidityAirMin
                bindingWarningSetting.npHumidityLandMaxWarningSetting.value = warning.humidityLandMax
                bindingWarningSetting.npHumidityLandMinWarningSetting.value = warning.humidityLandMin

                // Cập nhật trạng thái của các checkbox
                bindingWarningSetting.cbTempMaxWarningSetting.isChecked = warning.tempStatusMax == 1
                bindingWarningSetting.cbTempMinWarningSetting.isChecked = warning.tempStatusMin == 1
                bindingWarningSetting.cbHumidityAirMaxWarningSetting.isChecked = warning.humidityAirStatusMax == 1
                bindingWarningSetting.cbHumidityAirMinWarningSetting.isChecked = warning.humidityAirStatusMin == 1
                bindingWarningSetting.cbHumidityLandMaxWarningSetting.isChecked = warning.humidityLandStatusMax == 1
                bindingWarningSetting.cbHumidityLandMinWarningSetting.isChecked = warning.humidityLandStatusMin == 1

                // Cập nhật trạng thái kích hoạt của các control dựa vào trạng thái switch
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

                val isEnabled = warning.status == 1
                listWarningCheckBox.forEach { it.isEnabled = isEnabled }
                listWarningNumberPicker.forEach { it.isEnabled = isEnabled }
            } finally {
                isUpdatingFromFirebase = false
            }
        }

        // Đăng ký listener với Firebase
        FirebaseWarningConfig.addListener(warningListener!!)

        lifecycleScope.launch {
            // Khởi tạo listener cho thông báo
            notificationListener = { data ->
                isUpdatingFromFirebase = true
                try {
                    // Cập nhật trạng thái của switch trước
                    bindingNotificationSetting.swNotificationSetting.isChecked = data.status

                    // Cập nhật trạng thái của các checkbox
                    bindingNotificationSetting.cbTempNotificationSetting.isChecked = data.temp
                    bindingNotificationSetting.cbHumidityAirNotificationSetting.isChecked = data.humidityAir
                    bindingNotificationSetting.cbHumidityLandNotificationSetting.isChecked = data.humidityLand
                    bindingNotificationSetting.tvTimeNotificationSetting.text = getTimeString(data.time)

                    // Cập nhật trạng thái kích hoạt của các control dựa vào trạng thái switch
                    val listNotification = listOf(
                        bindingNotificationSetting.ibTimeNotificationSetting,
                        bindingNotificationSetting.cbTempNotificationSetting,
                        bindingNotificationSetting.cbHumidityAirNotificationSetting,
                        bindingNotificationSetting.cbHumidityLandNotificationSetting
                    )
                    listNotification.forEach { it.isEnabled = data.status }
                } finally {
                    isUpdatingFromFirebase = false
                }
            }

            // Đăng ký listener với Firebase
            FirebaseNotificationConfig.addListener(notificationListener!!)
        }
    }

    //--------------------------------------------------------Notification------------------------------
    /**
     * Lấy thông tin cấu hình thông báo từ UI
     * @param onComplete Callback được gọi khi đã lấy xong thông tin
     */
    private fun getNotificationFirebase(onComplete: (E_NotificationConfigFirebase) -> Unit) {
        val time = extractHourMinute(bindingNotificationSetting.tvTimeNotificationSetting.text.toString())
        val notification= E_NotificationConfigFirebase(
            bindingNotificationSetting.swNotificationSetting.isChecked,
            (time?.first ?: 0) * 60 + (time?.second ?: 0),
            bindingNotificationSetting.cbTempNotificationSetting.isChecked,
            bindingNotificationSetting.cbHumidityAirNotificationSetting.isChecked,
            bindingNotificationSetting.cbHumidityLandNotificationSetting.isChecked,
        )
        onComplete(notification)
    }

    /**
     * Cập nhật hoặc thêm mới cấu hình thông báo lên Firebase
     */
    private fun updateOrInsertNotification() {
        getNotificationFirebase{
            FirebaseNotificationConfig.updateNotificationConfig(it)
        }
    }

    /**
     * Lấy thời gian hiện tại theo định dạng "HH:mm dd/MM"
     * @return Chuỗi thời gian định dạng "HH:mm dd/MM"
     */
    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date())
    }

//--------------------------------------------------------Notification------------------------------
//--------------------------------------------------------Warning--------------------------------------

    /**
     * Lấy thông tin cấu hình cảnh báo từ UI
     * @param onComplete Callback được gọi khi đã lấy xong thông tin
     */
    private fun getWarningFirebase(onComplete: (E_WarningConfigFirebase) -> Unit) {
        val status= if(bindingWarningSetting.swWarningSetting.isChecked) 1 else 0
        val tempStatusMax= if(bindingWarningSetting.cbTempMaxWarningSetting.isChecked) 1 else 0
        val tempStatusMin= if(bindingWarningSetting.cbTempMinWarningSetting.isChecked) 1 else 0
        val humidityAirStatusMax= if(bindingWarningSetting.cbHumidityAirMaxWarningSetting.isChecked) 1 else 0
        val humidityAirStatusMin= if(bindingWarningSetting.cbHumidityAirMinWarningSetting.isChecked) 1 else 0
        val humidityLandStatusMax= if(bindingWarningSetting.cbHumidityLandMaxWarningSetting.isChecked) 1 else 0
        val humidityLandStatusMin= if(bindingWarningSetting.cbHumidityLandMinWarningSetting.isChecked) 1 else 0

        // Lấy FCM token để gửi thông báo
        FCMTokenManager.getToken { fcmToken ->
            val warningConfig = E_WarningConfigFirebase(
                fcmToken ?: "", // Nếu token null thì đặt chuỗi rỗng
                status,
                tempStatusMax,
                bindingWarningSetting.npTempMaxWarningSetting.value,
                tempStatusMin,
                bindingWarningSetting.npTempMinWarningSetting.value,
                humidityAirStatusMax,
                bindingWarningSetting.npHumidityAirMaxWarningSetting.value,
                humidityAirStatusMin,
                bindingWarningSetting.npHumidityAirMinWarningSetting.value,
                humidityLandStatusMax,
                bindingWarningSetting.npHumidityLandMaxWarningSetting.value,
                humidityLandStatusMin,
                bindingWarningSetting.npHumidityLandMinWarningSetting.value,
            )
            onComplete(warningConfig) // Trả kết quả qua callback
        }
    }

    /**
     * Cập nhật hoặc thêm mới cấu hình cảnh báo lên Firebase
     */
    private fun updateOrInsertWarning() {
        getWarningFirebase {
            FirebaseWarningConfig.updateWarningConfig(it)
        }
    }
//--------------------------------------------------------Warning--------------------------------------
}