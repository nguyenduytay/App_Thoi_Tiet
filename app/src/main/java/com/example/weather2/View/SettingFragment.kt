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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.weather2.Model.Entity.Notification
import com.example.weather2.Model.Entity.Warning
import com.example.weather2.Model.Entity.E_WarningConfigFirebase
import com.example.weather2.Model.Fribase.FirebaseWarningConfig
import com.example.weather2.Model.Fribase.FirebaseWeatherData
import com.example.weather2.View.Notification.FCMTokenManager
import com.example.weather2.View.Notification.NotificationHelper
import com.example.weather2.ViewModel.NotificationViewModel
import com.example.weather2.ViewModel.WarningViewModel
import com.example.weather2.databinding.FragmentSettingBinding
import com.example.weather2.databinding.NotificationSettingBinding
import com.example.weather2.databinding.WarningSettingBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SettingFragment : Fragment() {
    private lateinit var bindingSetting: FragmentSettingBinding
    private lateinit var bindingNotificationSetting: NotificationSettingBinding
    private lateinit var bindingWarningSetting: WarningSettingBinding
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var warningViewModel: WarningViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetting = FragmentSettingBinding.inflate(inflater, container, false)
        bindingNotificationSetting =
            NotificationSettingBinding.bind(bindingSetting.includeNotificationSetting.notificationSetting)
        bindingWarningSetting =
            WarningSettingBinding.bind(bindingSetting.includeWarningSetting.warningSetting)
        //khưởi tạo notificationHelper
        notificationHelper = NotificationHelper(requireContext())
        //khởi tạo viewmodel
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        warningViewModel = ViewModelProvider(this)[WarningViewModel::class.java]

        setNumberPicker()
        onEndOff()
        openTimePickerDialog()
        checkboxUpdateNotification()
        showNotification()
        showRoomDataBaseNotification()
        return bindingSetting.root
    }

    //sự kiện cấp số max và min numberPicker
    private fun setNumberPicker() {
        val list1 = listOf(
            bindingWarningSetting.npHumidityAirMaxWarningSetting,
            bindingWarningSetting.npHumidityAirMinWarningSetting,
            bindingWarningSetting.npHumidityLandMaxWarningSetting,
            bindingWarningSetting.npHumidityLandMinWarningSetting
        )
        val list2 = listOf(
            bindingWarningSetting.npTempMaxWarningSetting,
            bindingWarningSetting.npTempMinWarningSetting
        )
        for (i in list1) {
            i.maxValue = 100
            i.minValue = 0
            i.setFormatter { value -> "  $value % " }
            i.value = 89
            i.isEnabled = false
        }
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

    //sự kiện bật thông báo và cản báo
    private fun onEndOff() {
        val listNotification = listOf(
            bindingNotificationSetting.ibTimeNotificationSetting,
            bindingNotificationSetting.cbTempNotificationSetting,
            bindingNotificationSetting.cbHumidityAirNotificationSetting,
            bindingNotificationSetting.cbHumidityLandNotificationSetting
        )
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
        bindingNotificationSetting.swNotificationSetting.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                for (i in listNotification) {
                    i.isEnabled = true
                }
                updateOrInsertNotification(true)
            } else {
                for (i in listNotification) {
                    i.isEnabled = false
                }
                listNotification.takeLast(3).forEach {
                    if (it is CheckBox) {
                        it.isChecked = false
                    }
                }
                updateOrInsertNotification(false)
            }
        }
        bindingWarningSetting.swWarningSetting.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                for (i in listWarningCheckBox) {
                    i.isEnabled = true
                }
                updateOrInsertWarning(true)
            } else {
                for (i in listWarningCheckBox) {
                    i.isEnabled = false
                    i.isChecked = false
                }
               updateOrInsertWarning(false)
            }
        }
        for (i in listWarningCheckBox.indices) {
            listWarningCheckBox[i].setOnCheckedChangeListener { _, isChecked ->
                listWarningNumberPicker[i].isEnabled = isChecked
                updateOrInsertWarning(true)
            }
            listWarningNumberPicker[i].isEnabled = listWarningCheckBox[i].isChecked
        }
        for(i in listWarningNumberPicker)
        {
            i.setOnValueChangedListener{
                _,_,_ ->
                updateOrInsertWarning(true)
            }
        }
    }

    //hàm đặt thời gian để thông báo
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
                    val hourString=if(selectedHour<10) "0$selectedHour" else "$selectedHour"
                    val minuteString=if(selectedMinute<10) "0$selectedMinute" else "$selectedMinute"
                    bindingNotificationSetting.tvTimeNotificationSetting.text = "$hourString : $minuteString"
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
    //hàm tách thời gian về chuỗi
    private fun getTimeString(time: Int): String {
        val house = time / 60
        val minute = time % 60
        val houseString = if (house < 10) ("0$house") else ("$house")
        val minuteString = if (minute < 10) ("0$minute") else ("$minute")
        return ("$houseString : $minuteString")
    }

    //hàm tách thời gian từ chuỗi về số
    fun extractHourMinute(timeString: String): Pair<Int, Int>? {
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

    //hàm chuyển đổi thời gian về giây
    private fun getTimeInMillis(timeInMinutes: Int): Long {
        val calendar = Calendar.getInstance()

        // Chuyển đổi từ phút sang giờ và phút
        val hour = timeInMinutes / 60
        val minute = timeInMinutes % 60

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Nếu thời gian đã trôi qua hôm nay, đặt lại cho ngày mai
//        if (calendar.timeInMillis < System.currentTimeMillis()) {
//            calendar.add(Calendar.DAY_OF_MONTH, 1)
//        }
        return calendar.timeInMillis
    }
//cập nhật dữu liệu đã lưu room database
    private fun showRoomDataBaseNotification()
{
        lifecycleScope.launch {
            warningViewModel.getWarning(1).collect{
                    warning ->
                bindingWarningSetting.swWarningSetting.isChecked=warning.status
                bindingWarningSetting.npTempMaxWarningSetting.value=warning.tempMax
                bindingWarningSetting.npTempMinWarningSetting.value=warning.tempMin
                bindingWarningSetting.npHumidityAirMaxWarningSetting.value=warning.humidityAirMax
                bindingWarningSetting.npHumidityAirMinWarningSetting.value=warning.humidityAirMin
                bindingWarningSetting.npHumidityLandMaxWarningSetting.value=warning.humidityLandMax
                bindingWarningSetting.npHumidityLandMinWarningSetting.value=warning.humidityLandMin
                bindingWarningSetting.cbTempMaxWarningSetting.isChecked=warning.tempStatusMax
                bindingWarningSetting.cbTempMinWarningSetting.isChecked=warning.tempStatusMin
                bindingWarningSetting.cbHumidityAirMaxWarningSetting.isChecked=warning.humidityAirStatusMax
                bindingWarningSetting.cbHumidityAirMinWarningSetting.isChecked=warning.humidityAirStatusMin
                bindingWarningSetting.cbHumidityLandMaxWarningSetting.isChecked=warning.humidityLandStatusMax
                bindingWarningSetting.cbHumidityLandMinWarningSetting.isChecked=warning.humidityLandStatusMin
            }
        }
    lifecycleScope.launch {
        notificationViewModel.getNotification(1).collect{
                notification ->
            bindingNotificationSetting.swNotificationSetting.isChecked=notification.status
            bindingNotificationSetting.cbTempNotificationSetting.isChecked=notification.temp
            bindingNotificationSetting.cbHumidityAirNotificationSetting.isChecked=notification.humidityAir
            bindingNotificationSetting.cbHumidityLandNotificationSetting.isChecked=notification.humidityLand
            bindingNotificationSetting.tvTimeNotificationSetting.text=getTimeString(notification.time)
        }
    }
}
    //--------------------------------------------------------Notification------------------------------
    // lấy thông tin lưu Notification vào roomdatabase
    private fun getNotification(status: Boolean): Notification? {
        val time =
            extractHourMinute(bindingNotificationSetting.tvTimeNotificationSetting.text.toString())
                ?: return null
        return Notification(
            1,
            time.first * 60 + time.second,
            bindingNotificationSetting.cbTempNotificationSetting.isChecked,
            bindingNotificationSetting.cbHumidityAirNotificationSetting.isChecked,
            bindingNotificationSetting.cbHumidityLandNotificationSetting.isChecked,
            status
        )
    }

    //hàm thêm dữu liệu vào room database
    private fun updateOrInsertNotification(status: Boolean) {
        lifecycleScope.launch {
            notificationViewModel.getNotification(1).firstOrNull()?.let { existingTimer ->
                val updatedNotification = getNotification(status)?.copy(id = existingTimer.id)
                updatedNotification?.let { notificationViewModel.updateNotification(it) }
            } ?: run {
                getNotification(status)?.let { notificationViewModel.insert(it) }
            }
        }
    }

    //hàm sự kiện click update notification cho checkbox
    private fun checkboxUpdateNotification() {
        val list = listOf(
            bindingNotificationSetting.cbTempNotificationSetting,
            bindingNotificationSetting.cbHumidityAirNotificationSetting,
            bindingNotificationSetting.cbHumidityLandNotificationSetting
        )
        for (i in list) {
            i.setOnCheckedChangeListener { _, checked ->
                updateOrInsertNotification(checked)
            }
        }
    }
    //hàm câp nhật thời gian
    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date())
    }
    //hàm hiển thị thông báo
    private fun showNotification() {
        lifecycleScope.launch {
            notificationViewModel.getNotification(1).collect { notification ->
                if (notification.status) { // Nếu thông báo được bật
                    val temp = if (notification.temp)
                        "🌡 Nhiệt độ: ${FirebaseWeatherData.getWeatherData().temperature}℃" else ""
                    val humidityAir = if (notification.humidityAir)
                        "💧 Độ ẩm không khí: ${FirebaseWeatherData.getWeatherData().humidity}%" else ""
                    val humidityLand = if (notification.humidityLand)
                        "🌱 Độ ẩm đất: ${FirebaseWeatherData.getWeatherData().humidityLand}%" else ""

                    val message = listOf(temp, humidityAir, humidityLand)
                        .filter { it.isNotEmpty() }
                        .joinToString("\n")

                    if (message.isNotEmpty()) {
                        // Sử dụng phiên bản showNotification mới hỗ trợ nội dung dài
                        notificationHelper.showBigStyleNotification(
                            title = "Thông báo thời tiết",
                            bigText = message,
                            summaryText = "Cập nhật lúc ${getCurrentTime()}"
                        )
                        val intervalMillis = notification.time.toLong() * 60 * 1000
                        notificationHelper.scheduleNotification(requireContext(), intervalMillis, message)
                    } else {
                        notificationHelper.cancelScheduledNotification(requireContext())
                    }
                } else {
                    notificationHelper.cancelScheduledNotification(requireContext())
                }
            }
        }
    }
//--------------------------------------------------------Notification------------------------------
//--------------------------------------------------------Warning--------------------------------------

//    //lấy thông tin màn hình lưu vào roomdatabase
    private fun getWarningSetting(status : Boolean) : Warning
    {
        return Warning(
            1,
            bindingWarningSetting.npTempMaxWarningSetting.value,
            bindingWarningSetting.cbTempMaxWarningSetting.isChecked,
            bindingWarningSetting.npTempMinWarningSetting.value,
            bindingWarningSetting.cbTempMinWarningSetting.isChecked,
            bindingWarningSetting.npHumidityAirMaxWarningSetting.value,
            bindingWarningSetting.cbHumidityAirMaxWarningSetting.isChecked,
            bindingWarningSetting.npHumidityAirMinWarningSetting.value,
            bindingWarningSetting.cbHumidityAirMinWarningSetting.isChecked,
            bindingWarningSetting.npHumidityLandMaxWarningSetting.value,
            bindingWarningSetting.cbHumidityLandMaxWarningSetting.isChecked,
            bindingWarningSetting.npHumidityLandMinWarningSetting.value,
            bindingWarningSetting.cbHumidityLandMinWarningSetting.isChecked,
            status
        )
    }
    //hàm lấy dữu liệu lưu vào firebase
    private fun getWarningFirebase(onComplete: (E_WarningConfigFirebase) -> Unit) {
        val tempStatusMax= if(bindingWarningSetting.cbTempMaxWarningSetting.isChecked) 1 else 0
        val tempStatusMin= if(bindingWarningSetting.cbTempMinWarningSetting.isChecked) 1 else 0
        val humidityAirStatusMax= if(bindingWarningSetting.cbHumidityAirMaxWarningSetting.isChecked) 1 else 0
        val humidityAirStatusMin= if(bindingWarningSetting.cbHumidityAirMinWarningSetting.isChecked) 1 else 0
        val humidityLandStatusMax= if(bindingWarningSetting.cbHumidityLandMaxWarningSetting.isChecked) 1 else 0
        val humidityLandStatusMin= if(bindingWarningSetting.cbHumidityLandMinWarningSetting.isChecked) 1 else 0
        FCMTokenManager.getToken { fcmToken ->
            val warningConfig = E_WarningConfigFirebase(
                fcmToken ?: "", // Nếu token null thì đặt chuỗi rỗng
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
    //    //hàm thêm dữu liệu vào room database
    private fun updateOrInsertWarning(status: Boolean) {
        lifecycleScope.launch {
            warningViewModel.getWarning(1).firstOrNull()?.let { warning ->
                val updatedWarning = getWarningSetting(status).copy(id = warning.id)
                updatedWarning.let { warningViewModel.updateWarning(it) }
                getWarningFirebase {
                    FirebaseWarningConfig.updateWarningConfig(it)
                }
            } ?: run {
                getWarningSetting(status).let { warningViewModel.insert(it) }
            }
        }
    }
//--------------------------------------------------------Warning--------------------------------------
}