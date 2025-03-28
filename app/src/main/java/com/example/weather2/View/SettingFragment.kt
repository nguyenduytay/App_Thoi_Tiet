package com.example.weather2.View

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.example.weather2.R
import com.example.weather2.databinding.FragmentSettingBinding
import com.example.weather2.databinding.FragmentSystemBinding
import com.example.weather2.databinding.NotificationSettingBinding
import com.example.weather2.databinding.WarningSettingBinding

class SettingFragment : Fragment() {
    private lateinit var bindingSetting: FragmentSettingBinding
    private lateinit var bindingNotificationSetting: NotificationSettingBinding
    private lateinit var bindingWarningSetting: WarningSettingBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetting = FragmentSettingBinding.inflate(inflater, container, false)
        bindingNotificationSetting =
            NotificationSettingBinding.bind(bindingSetting.includeNotificationSetting.notificationSetting)
        bindingWarningSetting =
            WarningSettingBinding.bind(bindingSetting.includeWarningSetting.warningSetting)
        setNumberPicker()
        onEndOff()
        openTimePickerDialog()
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
        val listWarningNumberPicker= listOf(
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
            } else {
                for (i in listNotification) {
                    i.isEnabled = false
                }
                listNotification.takeLast(3).forEach {
                    if (it is CheckBox) {
                        it.isChecked = false
                    }
                }
            }
        }
        bindingWarningSetting.swNotificationSetting.setOnCheckedChangeListener { _, checked ->
            if(checked)
            {
                for(i in listWarningCheckBox)
                {
                    i.isEnabled=true
                }
            }else
            {
                for(i in listWarningCheckBox)
                {
                    i.isEnabled=false
                    i.isChecked=false
                }
            }
        }
        for (i in listWarningCheckBox.indices) {
            listWarningCheckBox[i].setOnCheckedChangeListener { _, isChecked ->
                listWarningNumberPicker[i].isEnabled = isChecked
            }
            listWarningNumberPicker[i].isEnabled = listWarningCheckBox[i].isChecked
        }
    }
    //hàm đặt thời gian để thông báo
    @SuppressLint("SetTextI18n")
    private fun openTimePickerDialog() {
        bindingNotificationSetting.ibTimeNotificationSetting.setOnClickListener {
            val hour = 23
            val minute = 55
            val is24HourView = true

            val timePickerDialog = TimePickerDialog(
                requireContext(),
                android.R.style.Theme_Holo_Light_Dialog,
                { _, selectedHour, selectedMinute ->
                    bindingNotificationSetting.tvTimeNotificationSetting.setText("$selectedHour:$selectedMinute")
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
}