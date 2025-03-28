package com.example.weather2.View

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.weather2.Model.FirebaseRepository
import com.example.weather2.databinding.ActivateSystemBinding
import com.example.weather2.databinding.FragmentSystemBinding
import com.example.weather2.databinding.ParameterSystemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.*
import kotlin.math.roundToInt

class SystemFragment : Fragment() {
    private lateinit var bindingSystem: FragmentSystemBinding
    private lateinit var bindingParameterSystem: ParameterSystemBinding
    private lateinit var bindingActivateSystem: ActivateSystemBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSystem = FragmentSystemBinding.inflate(inflater, container, false)
        bindingParameterSystem =
            ParameterSystemBinding.bind(bindingSystem.includeParameterSystem.parameterSystem)
        bindingActivateSystem =
            ActivateSystemBinding.bind(bindingSystem.includeActivateSystem.activateSystem)
        getTime()
        blinkSecond()
        timeTimer()
        setSpinner()
        onEndOffSwitch()
        updateWeather()
        return bindingSystem.root
    }

    //hàm cập nhật thời gian thực
    private fun getTime() {
        lifecycleScope.launch {
            while (isActive) {
                bindingSystem.tvTimeSystem.text = getCurrentTime().first
                bindingSystem.tvTimeFormatSystem.text = getCurrentTime().second
                bindingSystem.tvRankDaySystem.text = getCurrentDayInVN()
                bindingSystem.tvYearSystem.text = getCurrentDayMonthEndYear().second
                bindingSystem.tvDayMonthSystem.text = getCurrentDayMonthEndYear().first
                delay(10)
            }
        }
    }

    // hàm lấy thứ trong tuần bằng tiếng việt
    private fun getCurrentDayInVN(): String {
        val calendar = Calendar.getInstance()
        val daysOfWeek = arrayOf(
            "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
        )
        return daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    //hàm cập nhật thời gian liên tục
    private fun getCurrentTime(): Pair<String, String> {
        val sdfTime = SimpleDateFormat("hh : mm", Locale.getDefault())
        val sdfAmPm = SimpleDateFormat("a", Locale.getDefault())

        val time = sdfTime.format(Date())
        val amPm = sdfAmPm.format(Date())
        return Pair(time, amPm)
    }

    //hàm cập nhật ngày tháng liên tục
    private fun getCurrentDayMonthEndYear(): Pair<String, String> {
        val sdfDayMonth = SimpleDateFormat("dd ' - Tháng: ' MM", Locale("vi", "VN"))
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())

        val dayMonth = sdfDayMonth.format(Date())
        val year = sdfYear.format(Date())
        return Pair(dayMonth, year)
    }

    //hàm nháy giây
    private fun blinkSecond() {
        val blinkSecondTextList: List<TextView> = listOf(
            bindingSystem.tvTimeSecond1System,
            bindingSystem.tvTimeSecond2System,
            bindingSystem.tvTimeSecond3System,
            bindingSystem.tvTimeSecond4System,
            bindingSystem.tvTimeSecond5System,
            bindingSystem.tvTimeSecond6System
        )

        lifecycleScope.launch(Dispatchers.Default) {
            blinkSecondTextList.forEach { tv ->
                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F04E5E"))
                delay(300)
            }
            delay(500)
            blinkSecondTextList.reversed().forEach { tv ->
                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D9D9D9"))
                delay(300)
            }
            while (true) {
                blinkSecondTextList.forEach { tv ->
                    withContext(Dispatchers.Main) {
                        tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F04E5E"))
                    }
                    delay(1000)
                }
                delay(1000)
                blinkSecondTextList.reversed().forEach { tv ->
                    withContext(Dispatchers.Main) {
                        tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D9D9D9"))
                    }
                    delay(1000)
                }
                delay(1000)
            }
        }
    }
    //hàm nháy giây
//    private fun blinkSecond() {
//        val blinkSecondTextList: List<TextView> = listOf(
//            bindingSystem.tvTimeSecond1System,
//            bindingSystem.tvTimeSecond2System,
//            bindingSystem.tvTimeSecond3System,
//            bindingSystem.tvTimeSecond4System,
//            bindingSystem.tvTimeSecond5System,
//            bindingSystem.tvTimeSecond6System
//        )
//
//        lifecycleScope.launch(Dispatchers.Default) {
//            blinkSecondTextList.forEach { tv ->
//                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F04E5E"))
//                delay(300)
//            }
//            delay(500)
//            blinkSecondTextList.reversed().forEach { tv ->
//                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D9D9D9"))
//                delay(300)
//            }
//            while (true) {
//                val currentSecond = Calendar.getInstance().get(Calendar.SECOND)
//                val activeIndex = currentSecond / 10
//
//                withContext(Dispatchers.Main) {
//                    blinkSecondTextList.forEachIndexed { index, tv ->
//                        val color = if (index <= activeIndex) "#F04E5E" else "#D9D9D9"
//                        tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
//                    }
//                }
//
//                delay(1000)
//            }
//        }
//    }
    //hàm hẹn giờ báo động
    @SuppressLint("SimpleDateFormat")
    private fun timeTimer() {
        var startTime: Calendar? = null
        var endTime: Calendar? = null

        bindingActivateSystem.btTimerStartSystem.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                startTime = cal.clone() as Calendar // Lưu thời gian bắt đầu

                bindingActivateSystem.tvTimerStartSystem.text =
                    SimpleDateFormat("HH:mm").format(cal.time)
            }
            TimePickerDialog(
                requireContext(),
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        bindingActivateSystem.btTimerEndSystem.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)

                if (startTime != null && cal.timeInMillis <= startTime!!.timeInMillis) {
                    Toast.makeText(requireContext(), "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!", Toast.LENGTH_SHORT).show()
                    return@OnTimeSetListener
                }

                endTime = cal.clone() as Calendar
                bindingActivateSystem.tvTimerEndSystem.text =
                    SimpleDateFormat("HH:mm").format(cal.time)
            }
            TimePickerDialog(
                requireContext(),
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    //hàm thêm sự kiện lựa chọn thứ vào danh sách
    private fun setSpinner() {
        val items = arrayOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật")
        val shortItems = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dayOrderMap = mapOf("T2" to 1, "T3" to 2, "T4" to 3, "T5" to 4, "T6" to 5, "T7" to 6, "CN" to 7)
        val selectItems = BooleanArray(items.size)
        val selectList = mutableListOf<String>()

        bindingActivateSystem.btSelectTimerSystem.setOnClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setTitle("Chọn các mục")
                setMultiChoiceItems(items, selectItems) { _, which, isChecked ->
                    selectItems[which] = isChecked
                    if (isChecked) {
                        if (!selectList.contains(shortItems[which])) {
                            selectList.add(shortItems[which])
                        }
                    } else {
                        selectList.remove(shortItems[which])
                    }
                }
                setPositiveButton("OK") { _, _ ->
                    val sortedList=selectList.sortedBy { dayOrderMap[it]?:Int.MAX_VALUE }
                    bindingActivateSystem.tvSelectDayTimerSystem.text =
                        if (selectList.isNotEmpty())
                        {
                            if(selectList.size==7) "Mỗi ngày"
                            else
                            sortedList.joinToString(", ")
                        }
                        else "Hôm nay"
                }
                setNegativeButton("Hủy", null)
            }.show()
        }
    }
    //sự kiện bật tắt hẹn giờ
    private fun onEndOffSwitch() {
        bindingActivateSystem.swTimeTimerSystem.setOnCheckedChangeListener { _, isChecked ->
            bindingActivateSystem.btTimerStartSystem.isEnabled = isChecked
            bindingActivateSystem.btTimerEndSystem.isEnabled = isChecked
            bindingActivateSystem.btSelectTimerSystem.isEnabled=isChecked
        }
    }
    //sự kiên cập nhật thời tiết thay đổi liên tục
    private fun updateWeather()
    {
        FirebaseRepository.addListener {
            weatherData->
            bindingParameterSystem.tvTempParameterSystem.text=weatherData.temperature.toString().plus(" ℃")
            bindingParameterSystem.tvHumidityAirParameterSystem.text=weatherData.humidity.toString().plus(" %")
            bindingParameterSystem.tvHumidityLandParameterSystem.text=weatherData.humidity.toString().plus(" %")

            bindingParameterSystem.sbTempParameterSystem.progress =
                (weatherData.temperature?.roundToInt()?.plus(25)) ?: 0
            bindingParameterSystem.sbHumidityAirParameterSystem.progress =
                weatherData.humidity?.roundToInt() ?: 0
            bindingParameterSystem.sbHumidityLandParameterSystem.progress =
                weatherData.humidityLand?.roundToInt() ?: 0
        }
    }
}