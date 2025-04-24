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
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.weather2.Model.Fribase.FirebaseWeatherData
import com.example.weather2.Model.Fribase.FirebaseWatering
import com.example.weather2.Model.Fribase.FirebaseWindowBlinds
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
 * Hiển thị thời gian, thông tin thời tiết và các điều khiển hẹn giờ
 */
class SystemFragment : Fragment() {
    // Binding để truy cập các thành phần UI trong layout
    private lateinit var bindingSystem: FragmentSystemBinding
    private lateinit var bindingParameterSystem: ParameterSystemBinding
    private lateinit var bindingActivateSystem: ActivateSystemBinding

    // Lưu trữ các coroutine job để có thể hủy khi cần thiết, tránh memory leak
    private var timeUpdateJob: Job? = null
    private var blinkSecondJob: Job? = null
    private var timerProgressUpdateJob: Job? = null

    // Biến cờ để kiểm soát việc cập nhật từ Firebase, tránh vòng lặp vô hạn
    private var isUpdatingFromFirebase = false

    /**
     * Khởi tạo giao diện và thiết lập các listener
     * Được gọi khi fragment được tạo và hiển thị lên màn hình
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Khởi tạo các binding để truy cập các thành phần UI
        bindingSystem = FragmentSystemBinding.inflate(inflater, container, false)
        bindingParameterSystem =
            ParameterSystemBinding.bind(bindingSystem.includeParameterSystem.parameterSystem)
        bindingActivateSystem =
            ActivateSystemBinding.bind(bindingSystem.includeActivateSystem.activateSystem)

        // Đầu tiên cập nhật dữ liệu từ Firebase
        updateWeather()

        // Thiết lập UI và các listener
        setSpinner()           // Thiết lập spinner chọn ngày trong tuần
        getTime()              // Khởi động hàm cập nhật thời gian thực
        blinkSecond()          // Khởi động hiệu ứng nhấp nháy giây
        timeTimer()            // Thiết lập chức năng hẹn giờ
        startTimerProgressUpdates()  // Theo dõi và cập nhật thanh tiến trình hẹn giờ
        OnAndOffWarning()      // Thiết lập chức năng bật tắt tưới nước
        wateringAutomatic()    // Thiết lập chức năng tưới nước tự động
        setupWindowBlindsControls()  // Thiết lập điều khiển rèm cửa

        return bindingSystem.root
    }

    /**
     * Hủy tất cả các coroutine khi Fragment bị hủy
     * Giúp tránh leak memory và crash
     */
    override fun onDestroyView() {
        super.onDestroyView()

        // Hủy tất cả các coroutine khi Fragment bị hủy để tránh memory leak
        timeUpdateJob?.cancel()
        blinkSecondJob?.cancel()
        timerProgressUpdateJob?.cancel() // Cần thêm dòng này để hủy job cập nhật tiến trình hẹn giờ
    }

    /**
     * Cập nhật thời gian thực liên tục trên giao diện
     * Sử dụng coroutine để cập nhật thời gian mỗi giây
     */
    private fun getTime() {
        // Sử dụng repeatOnLifecycle để tự động hủy coroutine khi lifecycle thay đổi
        timeUpdateJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val currentTime = getCurrentTime()
                    val currentDayMonth = getCurrentDayMonthEndYear()

                    withContext(Dispatchers.Main) {
                        // Cập nhật các thành phần UI hiển thị thời gian
                        bindingSystem.tvTimeSystem.text = currentTime.first
                        bindingSystem.tvTimeFormatSystem.text = currentTime.second
                        bindingSystem.tvRankDaySystem.text = getCurrentDayInVN()
                        bindingSystem.tvYearSystem.text = currentDayMonth.second
                        bindingSystem.tvDayMonthSystem.text = currentDayMonth.first
                    }
                    delay(1000) // Cập nhật mỗi giây thay vì 10ms để giảm tài nguyên sử dụng
                }
            }
        }
    }

    /**
     * Lấy thứ trong tuần bằng tiếng Việt dựa trên ngày hiện tại
     * @return Chuỗi thứ trong tuần bằng tiếng Việt (Ví dụ: "Thứ Hai", "Chủ nhật")
     */
    private fun getCurrentDayInVN(): String {
        val calendar = Calendar.getInstance()
        val daysOfWeek = arrayOf(
            "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
        )
        return daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    /**
     * Lấy thứ hiện tại dưới dạng viết tắt (T2, T3,...)
     * @return Chuỗi thứ viết tắt (Ví dụ: "T2", "CN")
     */
    private fun getDayShort(): String {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
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
     * Cập nhật thời gian hiện tại
     * @return Pair chứa chuỗi thời gian (hh : mm) và định dạng AM/PM
     */
    private fun getCurrentTime(): Pair<String, String> {
        val sdfTime = SimpleDateFormat("hh : mm", Locale.getDefault())
        val sdfAmPm = SimpleDateFormat("a", Locale.getDefault())

        val time = sdfTime.format(Date())
        val amPm = sdfAmPm.format(Date())
        return Pair(time, amPm)
    }

    /**
     * Cập nhật ngày tháng hiện tại
     * @return Pair chứa chuỗi ngày tháng và năm
     */
    private fun getCurrentDayMonthEndYear(): Pair<String, String> {
        val sdfDayMonth = SimpleDateFormat("dd ' - Tháng: ' MM", Locale("vi", "VN"))
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())

        val dayMonth = sdfDayMonth.format(Date())
        val year = sdfYear.format(Date())
        return Pair(dayMonth, year)
    }

    /**
     * Tạo hiệu ứng nhấp nháy giây trên UI
     * Đổi màu các điểm giây mỗi giây
     */
    //hàm nháy giây với hiệu ứng mượt mà
    private fun blinkSecond() {
        val blinkSecondTextList: List<TextView> = listOf(
            bindingSystem.tvTimeSecond1System,
            bindingSystem.tvTimeSecond2System,
            bindingSystem.tvTimeSecond3System,
            bindingSystem.tvTimeSecond4System,
            bindingSystem.tvTimeSecond5System,
            bindingSystem.tvTimeSecond6System
        )

        blinkSecondJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    // Sáng dần từ 1 đến 6
                    for (i in blinkSecondTextList.indices) {
                        withContext(Dispatchers.Main) {
                            blinkSecondTextList[i].backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor("#F04E5E"))
                        }
                        delay(100) // Tạo độ trễ giữa mỗi chấm sáng
                    }

                    delay(300) // Dừng lại một chút khi tất cả đã sáng

                    // Tắt dần từ 6 về 1
                    for (i in blinkSecondTextList.indices.reversed()) {
                        withContext(Dispatchers.Main) {
                            blinkSecondTextList[i].backgroundTintList =
                                ColorStateList.valueOf(Color.parseColor("#D9D9D9"))
                        }
                        delay(100) // Tạo độ trễ giữa mỗi chấm tắt
                    }

                    delay(300) // Dừng lại một chút khi tất cả đã tắt
                }
            }
        }
    }

    /**
     * Thiết lập chức năng đặt thời gian bắt đầu và kết thúc tưới nước
     * Sử dụng TimePickerDialog để người dùng chọn thời gian
     */
    @SuppressLint("SimpleDateFormat")
    private fun timeTimer() {
        var startTime: Calendar? = null
        var endTime: Calendar? = null

        // Thiết lập nút chọn thời gian bắt đầu
        bindingActivateSystem.btTimerStartSystem.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                startTime = cal.clone() as Calendar
                val timeFormatted = SimpleDateFormat("HH : mm").format(cal.time)
                bindingActivateSystem.tvTimerStartSystem.text = timeFormatted

                // Nếu đã bật timer, cập nhật firebase
                if (bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    FirebaseWatering.setWateringTimerStart(hour * 60 + minute)
                }
            }
            TimePickerDialog(
                requireContext(),
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Thiết lập nút chọn thời gian kết thúc
        bindingActivateSystem.btTimerEndSystem.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)

                // Kiểm tra đã chọn thời gian bắt đầu chưa
                if (startTime == null) {
                    Toast.makeText(
                        requireContext(),
                        "Vui lòng chọn thời gian bắt đầu trước!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnTimeSetListener
                }

                // Kiểm tra thời gian kết thúc > thời gian bắt đầu
                if (cal.timeInMillis <= startTime!!.timeInMillis) {
                    Toast.makeText(
                        requireContext(),
                        "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnTimeSetListener
                }

                endTime = cal.clone() as Calendar
                val timeFormatted = SimpleDateFormat("HH : mm").format(cal.time)
                bindingActivateSystem.tvTimerEndSystem.text = timeFormatted

                // Nếu đã bật timer, cập nhật firebase
                if (bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    FirebaseWatering.setWateringTimerEnd(hour * 60 + minute)
                }
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
     * Thiết lập chức năng chọn các ngày trong tuần để tưới cây
     * Hiển thị dialog đa lựa chọn cho người dùng
     */
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
                    val selectedText =
                        if (selectList.size == 7) "Mỗi ngày" else sortedList.joinToString(", ")
                    bindingActivateSystem.tvSelectDayTimerSystem.text = selectedText

                    // Cập nhật Firebase nếu timer đã được bật
                    if (bindingActivateSystem.swTimeTimerSystem.isChecked) {
                        FirebaseWatering.setRepeat(selectedText)
                    }
                }
                setNegativeButton("Hủy", null)
            }.show()
        }
    }

    /**
     * Cập nhật dữ liệu thời tiết từ Firebase lên giao diện
     * Hiển thị nhiệt độ, độ ẩm không khí và độ ẩm đất
     */
    private fun updateWeather() {
        FirebaseWeatherData.addListener { weatherData ->
            try {
                // Cập nhật giá trị nhiệt độ và độ ẩm lên TextView
                bindingParameterSystem.tvTempParameterSystem.text =
                    weatherData.temperature?.toString()?.plus(" ℃") ?: "N/A"
                bindingParameterSystem.tvHumidityAirParameterSystem.text =
                    weatherData.humidity?.toString()?.plus(" %") ?: "N/A"
                bindingParameterSystem.tvHumidityLandParameterSystem.text =
                    weatherData.humidityLand?.toString()?.plus(" %") ?: "N/A"

                // Cập nhật giá trị lên thanh SeekBar
                bindingParameterSystem.sbTempParameterSystem.progress =
                    (weatherData.temperature?.roundToInt()?.plus(25)) ?: 0
                bindingParameterSystem.sbHumidityAirParameterSystem.progress =
                    weatherData.humidity?.roundToInt() ?: 0
                bindingParameterSystem.sbHumidityLandParameterSystem.progress =
                    weatherData.humidityLand?.roundToInt() ?: 0
            } catch (e: Exception) {
                // Xử lý lỗi nếu có
                Toast.makeText(context, "Lỗi cập nhật dữ liệu: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**
     * Khởi động coroutine cập nhật thanh tiến trình hẹn giờ
     * Được gọi từ onCreateView để bắt đầu cập nhật liên tục
     */
    private fun startTimerProgressUpdates() {
        // Hủy job cũ nếu đang chạy
        timerProgressUpdateJob?.cancel()

        // Khởi tạo job mới để cập nhật tiến trình hẹn giờ
        timerProgressUpdateJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    // Cập nhật UI từ trong coroutine
                    updateProgressOnWateringTimer()
                    delay(1000) // Cập nhật mỗi giây
                }
            }
        }
    }

    /**
     * Cập nhật thanh tiến trình cho chức năng hẹn giờ tưới nước
     * Hiển thị thanh tiến độ dựa vào thời gian và ngày đã đặt
     */
    private fun updateProgressOnWateringTimer() {
        try {
            if (!bindingActivateSystem.swTimeTimerSystem.isChecked) {
                // Nếu timer không bật đưa thanh về 0
                bindingParameterSystem.sbTimerParameterSystem.progress = 0
                return
            }
            // Kiểm tra xem hôm nay có phải là ngày để tưới nước không
            val isSelectDay = isWateringTimerOfDay()
            if (!isSelectDay) {
                // Nếu không phải ngày đã chọn, progress là 0
                bindingParameterSystem.sbTimerParameterSystem.progress = 0
                return
            }

            val startTimeText = bindingActivateSystem.tvTimerStartSystem.text.toString()
            val endTimeText = bindingActivateSystem.tvTimerEndSystem.text.toString()

            val startTime = extractHourMinute(startTimeText)
            val endTime = extractHourMinute(endTimeText)

            // Chuyển đổi thành số phút trong ngày
            val startTimeInMinutes = startTime?.first!! * 60 + startTime.second
            val endTimeInMinutes = endTime?.first!! * 60 + endTime.second

            // Thời gian hiện tại tính theo phút và giây (để có hiệu ứng mượt mà hơn)
            val calendar = Calendar.getInstance()
            val currentTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 +
                    calendar.get(Calendar.MINUTE) +
                    calendar.get(Calendar.SECOND) / 60.0

            // Tính toán progress dựa vào thời gian hiện tại và khoảng thời gian hẹn giờ
            val progress = if (currentTimeInMinutes < startTimeInMinutes) {
                // Chưa tới giờ bắt đầu
                100
            } else if (currentTimeInMinutes >= endTimeInMinutes) {
                // Hết giờ tưới nước
                0
            } else {
                // Đang nằm trong khoảng hẹn giờ tưới nước
                val totalTimeRange = endTimeInMinutes - startTimeInMinutes
                val currentTimeRange = currentTimeInMinutes - startTimeInMinutes
                val progressValue = 100 - ((currentTimeRange / totalTimeRange) * 100).toInt()
                progressValue.coerceIn(0, 100)
            }
            bindingParameterSystem.sbTimerParameterSystem.progress = progress
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Kiểm tra xem ngày hiện tại có nằm trong danh sách ngày đã chọn hay không
     * @return true nếu ngày hiện tại được chọn hoặc đã chọn "Mỗi ngày", false trong các trường hợp khác
     */
    private fun isWateringTimerOfDay(): Boolean {
        val selectDaysText = bindingActivateSystem.tvSelectDayTimerSystem.text.toString()

        // Nếu là mỗi ngày trả về true
        if (selectDaysText == "Mỗi ngày") {
            return true
        }

        // Lấy thời gian hiện tại dưới dạng viết tắt
        val currentDayShort = getDayShort()

        // Kiểm tra thứ hiện tại nằm trong thứ được chọn không
        return selectDaysText.contains(currentDayShort)
    }

    /**
     * Thiết lập các listener và chức năng bật/tắt tưới nước
     * Cập nhật và đồng bộ dữ liệu với Firebase
     */
    private fun OnAndOffWarning() {
        isUpdatingFromFirebase = true
        try {
            // Thêm listener cho trạng thái tưới nước từ Firebase
            FirebaseWatering.addStatusListener { data ->
                bindingActivateSystem.swWaterSystem.isChecked = data == 1
            }

            // Thêm listener cho chế độ tự động dựa trên độ ẩm đất
            FirebaseWatering.addStatusHumidityLandListener { data ->
                isUpdatingFromFirebase = true
                try {
                    bindingActivateSystem.swAutomaticSystem.isChecked = data == 1
                    bindingActivateSystem.npHumidityLandSystem.isEnabled = data == 1
                } finally {
                    isUpdatingFromFirebase = false
                }
            }

            // Thêm listener cho trạng thái hẹn giờ
            FirebaseWatering.addStatusTimerListener { data ->
                isUpdatingFromFirebase = true
                try {
                    val isTimerEnabled = data == 1
                    bindingActivateSystem.swTimeTimerSystem.isChecked = isTimerEnabled

                    // Cập nhật trạng thái các control liên quan
                    bindingActivateSystem.btTimerStartSystem.isEnabled = isTimerEnabled
                    bindingActivateSystem.btTimerEndSystem.isEnabled = isTimerEnabled
                    bindingActivateSystem.btSelectTimerSystem.isEnabled = isTimerEnabled
                } finally {
                    isUpdatingFromFirebase = false
                }
            }

            // Thêm listener cho thời gian bắt đầu
            FirebaseWatering.addTimerStartListener { data ->
                val timeString = getTimeString(data)
                bindingActivateSystem.tvTimerStartSystem.text = timeString
                bindingParameterSystem.tvTimerParameterSystem.text = timeString
            }

            // Thêm listener cho thời gian kết thúc
            FirebaseWatering.addTimerEndListener { data ->
                bindingActivateSystem.tvTimerEndSystem.text = getTimeString(data)
            }

            // Thêm listener cho các ngày lặp lại
            FirebaseWatering.addRepeatListener { data ->
                bindingActivateSystem.tvSelectDayTimerSystem.text = data
            }
        } finally {
            isUpdatingFromFirebase = false
        }

        // Thiết lập listener cho công tắc tưới nước thủ công
        bindingActivateSystem.swWaterSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromFirebase) {
                FirebaseWatering.setWateringStatus(if (checked) 1 else 0)
            }
        }

        // Thiết lập listener cho công tắc tưới nước tự động theo độ ẩm đất
        bindingActivateSystem.swAutomaticSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromFirebase) {
                FirebaseWatering.setWateringStatusHumidityLand(if (checked) 1 else 0)
                bindingActivateSystem.npHumidityLandSystem.isEnabled = checked

                if (checked) {
                    FirebaseWatering.setWateringHumidityLand(bindingActivateSystem.npHumidityLandSystem.value.toDouble())
                }
            }
        }

        // Thiết lập listener cho công tắc hẹn giờ tưới
        bindingActivateSystem.swTimeTimerSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromFirebase) {
                FirebaseWatering.setWateringStatusTimer(if (checked) 1 else 0)

                // Cập nhật trạng thái các control liên quan
                bindingActivateSystem.btTimerStartSystem.isEnabled = checked
                bindingActivateSystem.btTimerEndSystem.isEnabled = checked
                bindingActivateSystem.btSelectTimerSystem.isEnabled = checked

                // Cập nhật thời gian khi bật timer
                if (checked) {
                    val timeTextStart = bindingActivateSystem.tvTimerStartSystem.text.toString()
                    val timeStart = extractHourMinute(timeTextStart)

                    val timeTextEnd = bindingActivateSystem.tvTimerEndSystem.text.toString()
                    val timeEnd = extractHourMinute(timeTextEnd)

                    val repeat = bindingActivateSystem.tvSelectDayTimerSystem.text.toString()

                    if (timeStart != null) {
                        FirebaseWatering.setWateringTimerStart(timeStart.first * 60 + timeStart.second)
                    }

                    if (timeEnd != null) {
                        FirebaseWatering.setWateringTimerEnd(timeEnd.first * 60 + timeEnd.second)
                    }

                    if (repeat.isNotEmpty()) {
                        FirebaseWatering.setRepeat(repeat)
                    }
                }
            }
        }

        // Thiết lập listeners cho TextViews
        bindingActivateSystem.tvTimerStartSystem.doAfterTextChanged { text ->
            if (!isUpdatingFromFirebase) {
                val newTimeStart = extractHourMinute(text.toString())
                if (newTimeStart != null && bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    FirebaseWatering.setWateringTimerStart(newTimeStart.first * 60 + newTimeStart.second)
                }
            }
        }

        bindingActivateSystem.tvTimerEndSystem.doAfterTextChanged { text ->
            if (!isUpdatingFromFirebase) {
                val newTimeEnd = extractHourMinute(text.toString())
                if (newTimeEnd != null && bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    FirebaseWatering.setWateringTimerEnd(newTimeEnd.first * 60 + newTimeEnd.second)
                }
            }
        }

        // Thiết lập listener cho việc thay đổi giá trị ngày lặp lại
        bindingActivateSystem.tvSelectDayTimerSystem.doAfterTextChanged { text ->
            if (!isUpdatingFromFirebase) {
                val new = text.toString()
                if (bindingActivateSystem.swTimeTimerSystem.isChecked) {
                    FirebaseWatering.setRepeat(new)
                }
            }
        }
    }

    /**
     * Chuyển đổi thời gian từ số phút trong ngày thành chuỗi định dạng "HH : mm"
     * @param time Thời gian tính bằng phút từ đầu ngày, null nếu không có dữ liệu
     * @return Chuỗi định dạng "HH : mm", trả về "00 : 00" nếu time là null
     */
    private fun getTimeString(time: Int?): String {
        if (time == null) return "00 : 00"

        val house = time / 60
        val minute = time % 60
        val houseString = if (house < 10) ("0$house") else ("$house")
        val minuteString = if (minute < 10) ("0$minute") else ("$minute")
        return ("$houseString : $minuteString")
    }

    /**
     * Thiết lập các điều khiển và listeners cho chức năng tưới nước tự động
     * Khởi tạo giá trị NumberPicker cho độ ẩm đất và xử lý sự kiện
     */
    private fun wateringAutomatic() {
        isUpdatingFromFirebase = true
        try {
            // Khởi tạo trạng thái ban đầu từ Firebase
            bindingActivateSystem.swAutomaticSystem.isChecked =
                FirebaseWatering.getStatusHumidityLand()
            bindingActivateSystem.swWaterSystem.isChecked = FirebaseWatering.getStatus()

            // Thiết lập NumberPicker để chọn ngưỡng độ ẩm đất
            bindingActivateSystem.npHumidityLandSystem.let { i ->
                i.maxValue = 100
                i.minValue = 0
                i.setFormatter { value -> "  $value % " }
                i.value = 25
                i.isEnabled = bindingActivateSystem.swAutomaticSystem.isChecked
            }
        } finally {
            isUpdatingFromFirebase = false
        }

        // Đã thiết lập listener cho swAutomaticSystem trong OnAndOffWarning()

        // Xử lý sự kiện khi giá trị NumberPicker thay đổi
        bindingActivateSystem.npHumidityLandSystem.setOnValueChangedListener { _, _, newVal ->
            if (!isUpdatingFromFirebase && bindingActivateSystem.swAutomaticSystem.isChecked) {
                FirebaseWatering.setWateringHumidityLand(newVal.toDouble())
            }
        }
    }

    /**
     * Thiết lập các điều khiển và listeners cho chức năng rèm cửa
     * Xử lý đồng bộ hóa dữ liệu với Firebase
     */
    private fun setupWindowBlindsControls() {
        isUpdatingFromFirebase = true
        try {
            // Thêm listener cho trạng thái rèm cửa thủ công
            FirebaseWindowBlinds.addStatusListener { data ->
                isUpdatingFromFirebase = true
                try {
                    bindingActivateSystem.swWindowBlindsSystem.isChecked = data == 1
                } finally {
                    isUpdatingFromFirebase = false
                }
            }

            // Thêm listener cho trạng thái rèm cửa tự động
            FirebaseWindowBlinds.addStatusAutomaticListener { data ->
                isUpdatingFromFirebase = true
                try {
                    bindingActivateSystem.swWindowBlindsAutomaticSystem.isChecked = data == 1
                } finally {
                    isUpdatingFromFirebase = false
                }
            }
        } finally {
            isUpdatingFromFirebase = false
        }

        // Xử lý sự kiện thay đổi trạng thái rèm cửa thủ công
        bindingActivateSystem.swWindowBlindsSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromFirebase) {
                FirebaseWindowBlinds.setWindowBlindsStatus(if (checked) 1 else 0)
            }
        }

        // Xử lý sự kiện thay đổi trạng thái rèm cửa tự động
        bindingActivateSystem.swWindowBlindsAutomaticSystem.setOnCheckedChangeListener { _, checked ->
            if (!isUpdatingFromFirebase) {
                FirebaseWindowBlinds.setWindowBlindsStatusAutomatic(if (checked) 1 else 0)
            }
        }
    }
}