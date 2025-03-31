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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.example.weather2.Model.Fribase.FirebaseWeatherData
import com.example.weather2.Model.Entity.Timer
import com.example.weather2.Model.Fribase.FirebaseWatering
import com.example.weather2.View.Notification.NotificationHelper
import com.example.weather2.ViewModel.TimerViewModel
import com.example.weather2.databinding.ActivateSystemBinding
import com.example.weather2.databinding.FragmentSystemBinding
import com.example.weather2.databinding.ParameterSystemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class SystemFragment : Fragment() {
    private lateinit var bindingSystem: FragmentSystemBinding
    private lateinit var bindingParameterSystem: ParameterSystemBinding
    private lateinit var bindingActivateSystem: ActivateSystemBinding
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var timerViewModel: TimerViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSystem = FragmentSystemBinding.inflate(inflater, container, false)
        bindingParameterSystem =
            ParameterSystemBinding.bind(bindingSystem.includeParameterSystem.parameterSystem)
        bindingActivateSystem =
            ActivateSystemBinding.bind(bindingSystem.includeActivateSystem.activateSystem)
        // Khởi tạo NotificationHelper với context
        notificationHelper = NotificationHelper(requireContext())

        // Khởi tạo ViewModel
        timerViewModel = ViewModelProvider(this)[TimerViewModel::class.java]
        getTime()
        blinkSecond()
        timeTimer()
        setSpinner()
        onEndOffSwitch()
        updateWeather()
        notificationTimer()
        checkIsChecked()
        //hàm cập nhật thanh seekbar
        updateProgressBar()
        //hàm bật tắt tưới nước
        OnAndOffWarning()
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
    @SuppressLint("SimpleDateFormat")
    private fun timeTimer() {
        var startTime: Calendar? = null
        var endTime: Calendar? = null

        bindingActivateSystem.btTimerStartSystem.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                startTime = cal.clone() as Calendar
                bindingActivateSystem.tvTimerStartSystem.text =
                    SimpleDateFormat("HH : mm").format(cal.time)
                updateOrInsertTimer()
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

                if (startTime == null) {
                    Toast.makeText(
                        requireContext(),
                        "Vui lòng chọn thời gian bắt đầu trước!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnTimeSetListener
                }

                if (cal.timeInMillis <= startTime!!.timeInMillis) {
                    Toast.makeText(
                        requireContext(),
                        "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnTimeSetListener
                }

                endTime = cal.clone() as Calendar
                bindingActivateSystem.tvTimerEndSystem.text =
                    SimpleDateFormat("HH : mm").format(cal.time)
                updateOrInsertTimer()
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
    //hàm thêm dữu liệu vào room database
    private fun updateOrInsertTimer() {
        lifecycleScope.launch {
            timerViewModel.getTimer(1).firstOrNull()?.let { existingTimer ->
                val updatedTimer = getTimer(true)?.copy(id = existingTimer.id)
                updatedTimer?.let { timerViewModel.updateTimer(it) }
            } ?: run {
                getTimer(true)?.let { timerViewModel.insert(it) }
            }
        }
    }

    //hàm tách thời gian
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
    //hàm lấy thông tin hẹn giờ trên màn hình
    private fun getTimer(status: Boolean): Timer? {
        val timeTextStart = bindingActivateSystem.tvTimerStartSystem.text.toString()
        val timeStart = extractHourMinute(timeTextStart)

        val timeTextEnd = bindingActivateSystem.tvTimerEndSystem.text.toString()
        val timeEnd = extractHourMinute(timeTextEnd)

        if (timeStart == null || timeEnd == null) {
            return null
        }

        return Timer(
            id = 1,
            timeStart = timeStart.first * 60 + timeStart.second,
            timeEnd = timeEnd.first * 60 + timeEnd.second,
            repeat = bindingActivateSystem.tvSelectDayTimerSystem.text.toString(),
            status = status
        )
    }

    //hàm thêm sự kiện lựa chọn thứ vào danh sách
    private fun setSpinner() {
        val items = arrayOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật")
        val shortItems = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dayOrderMap =
            mapOf("T2" to 1, "T3" to 2, "T4" to 3, "T5" to 4, "T6" to 5, "T7" to 6, "CN" to 7)
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
                    val sortedList = selectList.sortedBy { dayOrderMap[it] ?: Int.MAX_VALUE }
                    bindingActivateSystem.tvSelectDayTimerSystem.text =
                            if (selectList.size == 7) "Mỗi ngày"
                            else
                                sortedList.joinToString(", ")
                    updateOrInsertTimer()
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
            bindingActivateSystem.btSelectTimerSystem.isEnabled = isChecked
            lifecycleScope.launch {
                val currentTimer = timerViewModel.getTimer(1).first()
                if (currentTimer.status != isChecked) {
                    timerViewModel.updateTimer(currentTimer.copy(status = isChecked))
                }
            }
        }
    }
    //sự kiên cập nhật thời tiết thay đổi liên tục
    private fun updateWeather() {
        FirebaseWeatherData.addListener { weatherData ->
            bindingParameterSystem.tvTempParameterSystem.text =
                weatherData.temperature.toString().plus(" ℃")
            bindingParameterSystem.tvHumidityAirParameterSystem.text =
                weatherData.humidity.toString().plus(" %")
            bindingParameterSystem.tvHumidityLandParameterSystem.text =
                weatherData.humidity.toString().plus(" %")

            bindingParameterSystem.sbTempParameterSystem.progress =
                (weatherData.temperature?.roundToInt()?.plus(25)) ?: 0
            bindingParameterSystem.sbHumidityAirParameterSystem.progress =
                weatherData.humidity?.roundToInt() ?: 0
            bindingParameterSystem.sbHumidityLandParameterSystem.progress =
                weatherData.humidityLand?.roundToInt() ?: 0
        }
    }
    //hàm sử lý nút bấm tưới nước
    private fun OnAndOffWarning()
    {
        FirebaseWatering.addListener { newStatus ->
            bindingActivateSystem.swWaterSystem.isChecked = newStatus != 0
        }
        bindingActivateSystem.swWaterSystem.setOnCheckedChangeListener{
            _,checked ->
            if(checked)
            {
                FirebaseWatering.setWateringStatus(1)
            }
            else
            {
                FirebaseWatering.setWateringStatus(0)
            }
        }
    }
        //hàm tách thời gian về chuỗi
        private fun getTimeString(time: Int): String {
            val house = time / 60
            val minute = time % 60
            val houseString= if(house<10) ("0$house") else("$house")
            val minuteString= if(minute<10) ("0$minute") else("$minute")
            return ("$houseString : $minuteString")
        }

    //kiểm tra nút nhấn hẹn giờ
    private fun checkIsChecked() {
        lifecycleScope.launch {
            timerViewModel.getTimer(1).collect { timer ->
                bindingActivateSystem.swTimeTimerSystem.isChecked = timer.status
                bindingActivateSystem.tvTimerStartSystem.text = getTimeString(timer.timeStart)
                bindingActivateSystem.tvTimerEndSystem.text = getTimeString(timer.timeEnd)
                bindingParameterSystem.tvTimerParameterSystem.text = getTimeString(timer.timeStart)
                bindingActivateSystem.tvSelectDayTimerSystem.text=timer.repeat
            }
        }
    }
    //hàm chuyển đổi thời gian
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

    //sự kiện hiện thông báo
    private fun notificationTimer() {
        lifecycleScope.launch {
            timerViewModel.getTimer(1).collect { timer ->
                // Lấy thứ hiện tại (Calendar.DAY_OF_WEEK)
                val calendar = Calendar.getInstance()
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                // Map thứ trong tuần sang định dạng "T2", "T3", ...
                val dayMap = mapOf(
                    Calendar.MONDAY to "T2",
                    Calendar.TUESDAY to "T3",
                    Calendar.WEDNESDAY to "T4",
                    Calendar.THURSDAY to "T5",
                    Calendar.FRIDAY to "T6",
                    Calendar.SATURDAY to "T7",
                    Calendar.SUNDAY to "CN"
                )
                val currentDay = dayMap[currentDayOfWeek] ?: return@collect

                // Lấy danh sách ngày từ Room (tách bằng ", ")
                val savedDays = timer.repeat.split(",").map { it.trim() }

                if (timer.status && (savedDays.contains(currentDay) || timer.repeat == "Mỗi ngày")) {
                    // Hủy thông báo cũ trước khi tạo mới
                    context?.let { WorkManager.getInstance(it).cancelAllWork() }

                    val triggerTimeMillisStart = getTimeInMillis(timer.timeStart)
                    val triggerTimeMillisEnd = getTimeInMillis(timer.timeEnd)

                    // Đặt lịch thông báo mới
                    notificationHelper.scheduleNotificationAtExactTime(
                        "⏰ Đến thời gian tưới nước rồi xếp ạ!",
                        triggerTimeMillisStart
                    )
                    notificationHelper.scheduleNotificationAtExactTime(
                        "⏰ Hết thời gian tưới nước rồi xếp ạ!",
                        triggerTimeMillisEnd
                    )
                } else {
                    // Hủy thông báo nếu hôm nay không nằm trong danh sách
                    context?.let { WorkManager.getInstance(it).cancelAllWork() }
                }
            }
        }
    }


    private var progressJob: Job? = null // Quản lý vòng lặp cập nhật SeekBar
    @SuppressLint("SetTextI18n")
    fun updateProgressBar() {
        lifecycleScope.launch {
            timerViewModel.getTimer(1)
                .stateIn(this) // Chuyển thành StateFlow để giữ giá trị mới nhất
                .collectLatest { timer -> // Lắng nghe dữ liệu từ Room

                    val calendar = Calendar.getInstance()
                    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                    val dayMap = mapOf(
                        Calendar.MONDAY to "T2",
                        Calendar.TUESDAY to "T3",
                        Calendar.WEDNESDAY to "T4",
                        Calendar.THURSDAY to "T5",
                        Calendar.FRIDAY to "T6",
                        Calendar.SATURDAY to "T7",
                        Calendar.SUNDAY to "CN"
                    )

                    val currentDay = dayMap[currentDayOfWeek] ?: return@collectLatest
                    val savedDays = timer.repeat.split(", ").map { it.trim() }

                    if (timer.status && (savedDays.contains(currentDay) || timer.repeat == "Mỗi ngày")) {
                        val sumTimerSec = (timer.timeEnd - timer.timeStart) * 60 // Tổng thời gian bằng giây
                        if (sumTimerSec <= 0) return@collectLatest // Tránh lỗi chia cho 0

                        progressJob?.cancel() // Hủy vòng lặp cũ nếu có
                        progressJob = launch {
                            while (isActive) { // Kiểm tra Job có còn hoạt động không
                                val now = Calendar.getInstance()
                                val currentTimeSec = now.get(Calendar.HOUR_OF_DAY) * 3600 +
                                        now.get(Calendar.MINUTE) * 60 +
                                        now.get(Calendar.SECOND)

                                val startSec = timer.timeStart * 60
                                val endSec = timer.timeEnd * 60

                                val progress = when {
                                    currentTimeSec < startSec -> 100 // Chưa đến giờ bắt đầu
                                    currentTimeSec >= endSec -> 0   // Hết thời gian tưới nước
                                    else -> (((endSec - currentTimeSec).toFloat() / sumTimerSec.toFloat()) * 100).toInt()
                                }

                                // Chỉ cập nhật UI nếu có thay đổi
                                if (bindingParameterSystem.sbTimerParameterSystem.progress != progress) {
                                    bindingParameterSystem.sbTimerParameterSystem.progress = progress
                                }
                                delay(1000) // Cập nhật mỗi giây
                            }
                        }
                    } else {
                        bindingParameterSystem.sbTimerParameterSystem.progress = 0
                        progressJob?.cancel() // Dừng cập nhật nếu không đúng ngày hoặc tắt timer
                    }
                }
        }
    }

}


