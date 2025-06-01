package com.example.weather2.View

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather2.Model.ItemAdapter.Weather24hData
import com.example.weather2.Model.ItemAdapter.Weather7dData
import com.example.weather2.R
import com.example.weather2.Adapter.Weather24hAdapter
import com.example.weather2.Adapter.Weather7dAdapter
import com.example.weather2.ViewModel.WeatherDataViewModel
import com.example.weather2.ViewModel.Weather24hViewModel
import com.example.weather2.ViewModel.Weather7dViewModel
import com.example.weather2.databinding.DayWeatherBinding
import com.example.weather2.databinding.FragmentWeatherBinding
import com.example.weather2.databinding.HourWeatherBinding
import com.example.weather2.databinding.StatusSunWeatherBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Fragment hiển thị thông tin thời tiết
 * Sử dụng MVVM pattern với Repository để quản lý dữ liệu
 */
class WeatherFragment : Fragment() {
    // Sử dụng nullable binding và property delegate để tránh memory leak
    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!! // Property delegate để truy cập binding an toàn

    // Binding cho các layout include - quản lý UI components riêng biệt
    private lateinit var bindingHourWeather: HourWeatherBinding // Binding cho RecyclerView dự báo theo giờ
    private lateinit var bindingStatusSunWeather: StatusSunWeatherBinding // Binding cho thông tin mặt trời
    private lateinit var bindingDayWeather: DayWeatherBinding // Binding cho RecyclerView dự báo theo ngày

    // ViewModels - quản lý dữ liệu và business logic theo MVVM pattern
    private lateinit var weatherDataViewModel: WeatherDataViewModel // ViewModel cho dữ liệu thời tiết hiện tại
    private lateinit var weather24hViewModel: Weather24hViewModel // ViewModel cho dự báo 24 giờ
    private lateinit var weather7dViewModel: Weather7dViewModel // ViewModel cho dự báo 7 ngày

    private var timeUpdateJob: Job? = null // Coroutine job để cập nhật thời gian real-time

    // Tọa độ Đà Nẵng - dùng để tính thời gian mặt trời mọc/lặn
    private val latitude = 16.0752 // Vĩ độ Đà Nẵng
    private val longitude = 108.1531 // Kinh độ Đà Nẵng

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UseCompatLoadingForDrawables") // Suppress warning về việc load drawable
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout và tạo binding
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)

        // Bind các include layout với binding tương ứng
        bindingHourWeather = HourWeatherBinding.bind(binding.includeHourWeather.hourWeather)
        bindingStatusSunWeather = StatusSunWeatherBinding.bind(binding.includeStatusSunWeather.statusSunWeather)
        bindingDayWeather = DayWeatherBinding.bind(binding.includeDayWeather.dayWeather)

        // Khởi tạo và cấu hình các thành phần
        initViewModels() // Tạo instances của các ViewModel
        setupObservers() // Thiết lập observers để lắng nghe dữ liệu từ ViewModel
        setupUI() // Thiết lập UI components

        return binding.root // Trả về root view của binding
    }

    /**
     * Khởi tạo các ViewModels sử dụng ViewModelProvider
     * ViewModelProvider đảm bảo ViewModels survive configuration changes
     */
    private fun initViewModels() {
        weatherDataViewModel = ViewModelProvider(this)[WeatherDataViewModel::class.java]
        weather24hViewModel = ViewModelProvider(this)[Weather24hViewModel::class.java]
        weather7dViewModel = ViewModelProvider(this)[Weather7dViewModel::class.java]
    }

    /**
     * Thiết lập tất cả observers để lắng nghe dữ liệu từ ViewModels
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        startTimeUpdates() // Bắt đầu cập nhật thời gian real-time
        observeCurrentWeatherData() // Lắng nghe dữ liệu thời tiết hiện tại
        observe24HourWeatherData() // Lắng nghe dữ liệu dự báo 24h
        observe7DayWeatherData() // Lắng nghe dữ liệu dự báo 7 ngày
        observeLoadingStates() // Lắng nghe trạng thái loading
        observeErrors() // Lắng nghe và xử lý lỗi
    }

    /**
     * Thiết lập các thành phần UI
     */
    private fun setupUI() {
        setupRecyclerViews() // Cấu hình RecyclerViews
        calculateAndDisplaySunTimes() // Tính toán và hiển thị thời gian mặt trời
    }

    /**
     * Bắt đầu coroutine để cập nhật thời gian và ngày hiện tại mỗi giây
     */
    private fun startTimeUpdates() {
        timeUpdateJob = viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ chạy khi Fragment ở trạng thái STARTED
                while (isActive) { // Lặp vô hạn trong khi coroutine active
                    binding.tvTime.text = getCurrentTime() // Cập nhật TextView thời gian
                    binding.tvDay.text = getCurrentDayInVN() // Cập nhật TextView ngày bằng tiếng Việt
                    delay(1000) // Delay 1 giây trước khi cập nhật tiếp
                }
            }
        }
    }

    /**
     * Observer dữ liệu thời tiết hiện tại từ ViewModel
     */
    private fun observeCurrentWeatherData() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                weatherDataViewModel.currentWeatherData.collect { weatherData -> // Collect Flow data
                    weatherData?.let { data -> // Nếu data không null
                        updateCurrentWeatherUI(data) // Cập nhật UI với dữ liệu mới
                    }
                }
            }
        }
    }

    /**
     * Observer dữ liệu dự báo 24 giờ từ ViewModel
     */
    private fun observe24HourWeatherData() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                weather24hViewModel.weather24hData.collect { weatherMap -> // Collect Map data
                    if (weatherMap.isNotEmpty()) { // Kiểm tra Map không rỗng
                        update24HourWeatherUI(weatherMap) // Cập nhật UI RecyclerView 24h
                        updateMinMaxTemperature(weatherMap) // Cập nhật nhiệt độ max/min từ data 24h
                    }
                }
            }
        }
    }

    /**
     * Observer dữ liệu dự báo 7 ngày từ ViewModel
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun observe7DayWeatherData() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                weather7dViewModel.weather7dData.collect { weatherMap -> // Collect Map data
                    if (weatherMap.isNotEmpty()) { // Kiểm tra Map không rỗng
                        update7DayWeatherUI(weatherMap) // Cập nhật UI RecyclerView 7 ngày
                    }
                }
            }
        }
    }

    /**
     * Observer trạng thái loading từ các ViewModels
     */
    private fun observeLoadingStates() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                weatherDataViewModel.isLoading.collect { isLoading -> // Collect loading state
                    // Xử lý hiển thị loading state
                    // Có thể show/hide progress bars tại đây nếu cần
                }
            }
        }
    }

    /**
     * Observer và xử lý lỗi từ ViewModels
     */
    private fun observeErrors() {
        viewLifecycleOwner.lifecycleScope.launch { // Launch coroutine tied to view lifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Chỉ observe khi Fragment active
                weatherDataViewModel.error.collect { error -> // Collect error messages
                    error?.let { // Nếu có lỗi
                        // Xử lý hiển thị lỗi cho user (có thể dùng Toast, Snackbar, etc.)
                        // Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        weatherDataViewModel.clearError() // Clear lỗi sau khi hiển thị
                    }
                }
            }
        }
    }

    /**
     * Cập nhật UI hiển thị thời tiết hiện tại
     */
    @SuppressLint("SetTextI18n") // Suppress warning về hardcoded string
    private fun updateCurrentWeatherUI(weatherData: com.example.weather2.Model.Entity.E_WeatherDataFirebase) {
        val currentTime = getCurrentTime() // Lấy thời gian hiện tại
        val rainProbability = weatherData.rain ?: 0 // Lấy xác suất mưa, default 0 nếu null

        // Cập nhật TextView nhiệt độ, sử dụng Elvis operator để handle null
        binding.tvTempHourLive.text = weatherData.temperature?.let {
            roundNumber(it).toString() + " ℃" // Format nhiệt độ với đơn vị
        } ?: "-- ℃" // Hiển thị placeholder nếu null

        // Cập nhật TextView độ ẩm, sử dụng Elvis operator để handle null
        binding.tvHumidyHourLive1.text = weatherData.humidity?.let {
            roundNumber(it).toString() + " %" // Format độ ẩm với đơn vị
        } ?: "-- %" // Hiển thị placeholder nếu null

        // Cập nhật icon thời tiết dựa trên thời gian và xác suất mưa
        val weatherIcon = getWeatherIcon(currentTime, rainProbability)
        binding.ivWeatherHourLive.setImageResource(weatherIcon) // Set resource cho ImageView
    }

    /**
     * Cập nhật UI RecyclerView hiển thị dự báo 24 giờ
     */
    private fun update24HourWeatherUI(weatherMap: Map<String, com.example.weather2.Model.Entity.E_Weather24hFirebase>) {
        // Transform Map thành List các Weather24hData objects
        val weatherList = weatherMap.entries.map { entry -> // Map each entry
            val time = extractHourMinute(entry.key) // Extract giờ:phút từ datetime string
            val weatherData = entry.value // Lấy weather data
            val rainProbability = calculateRainProbability(weatherData.rain) // Tính xác suất mưa

            // Tạo Weather24hData object cho adapter
            Weather24hData(
                time = time, // Thời gian đã format
                temperature = roundNumber(weatherData.temp), // Nhiệt độ đã làm tròn
                rainProbability = rainProbability, // Xác suất mưa đã tính
                icon = getWeatherIcon(time, rainProbability) // Icon phù hợp
            )
        }.sortedBy { // Sort list theo thời gian
            val timeParts = it.time.split(":") // Split "HH:mm" thành ["HH", "mm"]
            timeParts[0].toIntOrNull() ?: 0 // Convert hour thành Int để sort, default 0
        }

        // Cập nhật adapter của RecyclerView với dữ liệu mới
        bindingHourWeather.recyclerViewWeatherHour.adapter = Weather24hAdapter(weatherList)
    }

    /**
     * Cập nhật UI RecyclerView hiển thị dự báo 7 ngày
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun update7DayWeatherUI(weatherMap: Map<String, com.example.weather2.Model.Entity.E_Weather7dFirebase>) {
        // Transform Map thành List các Weather7dData objects
        val weatherList = weatherMap.entries.map { entry -> // Map each entry
            val dayName = extractDayOfWeekInVietnamese(entry.key) // Convert date thành tên thứ tiếng Việt
            val weatherData = entry.value // Lấy weather data
            val rainProbability = calculateRainProbability(weatherData.rain) // Tính xác suất mưa

            // Tạo Weather7dData object cho adapter
            Weather7dData(
                time = dayName, // Tên thứ bằng tiếng Việt
                temperatureMax = roundNumber(weatherData.temp_max), // Nhiệt độ max đã làm tròn
                temperatureMin = roundNumber(weatherData.temp_min), // Nhiệt độ min đã làm tròn
                rainProbability = rainProbability, // Xác suất mưa
                icon_morning = getWeatherIcon("6:00", rainProbability), // Icon buổi sáng
                icon_evening = getWeatherIcon("22:00", rainProbability) // Icon buổi tối
            )
        }.sortedBy { entry -> // Sort list theo ngày
            try {
                // Tìm key (date string) tương ứng với entry hiện tại
                val dateKey = weatherMap.entries.find {
                    extractDayOfWeekInVietnamese(it.key) == entry.time
                }?.key ?: ""
                // Parse thành LocalDate để so sánh
                LocalDate.parse(dateKey, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e: Exception) {
                LocalDate.now() // Fallback nếu parse lỗi
            }
        }

        // Cập nhật adapter của RecyclerView với dữ liệu mới
        bindingDayWeather.rvDayWeather.adapter = Weather7dAdapter(weatherList)
    }

    /**
     * Cập nhật nhiệt độ tối đa và tối thiểu trong ngày từ dữ liệu 24h
     */
    @SuppressLint("SetTextI18n") // Suppress warning về hardcoded string
    private fun updateMinMaxTemperature(weatherMap: Map<String, com.example.weather2.Model.Entity.E_Weather24hFirebase>) {
        val temperatures = weatherMap.values.map { it.temp } // Extract tất cả nhiệt độ thành List
        val maxTemp = temperatures.maxOrNull()?.toInt() // Tìm nhiệt độ cao nhất, convert thành Int
        val minTemp = temperatures.minOrNull()?.toInt() // Tìm nhiệt độ thấp nhất, convert thành Int

        // Cập nhật TextViews, sử dụng Elvis operator để handle null
        binding.tvTempDayMax1.text = maxTemp?.toString() ?: "--" // Hiển thị max temp hoặc placeholder
        binding.tvTempDayMin1.text = minTemp?.toString() ?: "--" // Hiển thị min temp hoặc placeholder
    }

    /**
     * Thiết lập cấu hình cho các RecyclerViews
     */
    private fun setupRecyclerViews() {
        // Cấu hình RecyclerView cho dự báo theo giờ
        bindingHourWeather.recyclerViewWeatherHour.apply {
            // Set LayoutManager horizontal cho scroll ngang
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // Cấu hình RecyclerView cho dự báo theo ngày
        bindingDayWeather.rvDayWeather.apply {
            // Set LayoutManager vertical cho scroll dọc
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
    }

    /**
     * Tính toán và hiển thị thời gian mặt trời mọc/lặn
     */
    private fun calculateAndDisplaySunTimes() {
        // Tính thời gian mặt trời mọc và lặn cho tọa độ Đà Nẵng
        val (sunriseStr, sunsetStr) = calculateSunTimes(latitude, longitude)
        // Cập nhật UI với thời gian đã tính
        bindingStatusSunWeather.tvStartSun.text = sunriseStr // Thời gian mặt trời mọc
        bindingStatusSunWeather.tvEndSun.text = sunsetStr // Thời gian mặt trời lặn
    }

    // =================================
    // UTILITY FUNCTIONS - Các hàm tiện ích
    // =================================

    /**
     * Lấy tên thứ trong tuần bằng tiếng Việt
     */
    private fun getCurrentDayInVN(): String {
        val calendar = Calendar.getInstance() // Lấy Calendar instance hiện tại
        // Array chứa tên các thứ bằng tiếng Việt
        val daysOfWeek = arrayOf(
            "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
        )
        // Trả về tên thứ tương ứng (Calendar.DAY_OF_WEEK bắt đầu từ 1)
        return daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    /**
     * Lấy thời gian hiện tại định dạng HH:mm
     */
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()) // Tạo formatter với pattern HH:mm
        return sdf.format(Date()) // Format Date hiện tại thành string
    }

    /**
     * Kiểm tra kết nối mạng
     */
    private fun isNetwork(): Boolean {
        // Lấy ConnectivityManager từ system service
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Lấy active network, return false nếu null
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        // Lấy capabilities của network, return false nếu null
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        // Kiểm tra có khả năng kết nối Internet
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Extract giờ và phút từ datetime string
     */
    @SuppressLint("NewApi", "DefaultLocale") // Suppress warnings về API level và locale
    fun extractHourMinute(dateTimeString: String): String {
        return try {
            // Tạo formatter với pattern yyyy-MM-dd HH:mm:ss
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            // Parse string thành LocalDateTime
            val dateTime = LocalDateTime.parse(dateTimeString, formatter)
            // Format thành HH:mm string với leading zero cho phút
            "${dateTime.hour}:${String.format("%02d", dateTime.minute)}"
        } catch (e: Exception) {
            "00:00" // Return default value nếu parse lỗi
        }
    }

    /**
     * Lấy weather icon dựa trên thời gian và xác suất mưa
     */
    fun getWeatherIcon(timeStr: String, rainProbability: Int): Int {
        // Extract giờ từ time string, default 12 nếu parse lỗi
        val hour = timeStr.split(":")[0].toIntOrNull() ?: 12
        // Xác định ban ngày (6:00 - 18:00) hay ban đêm
        val isDayTime = hour in 6..18

        // Return icon resource dựa trên xác suất mưa và thời gian
        return when {
            rainProbability > 60 -> R.mipmap.rain // Mưa nặng
            rainProbability > 40 -> {
                // Mưa vừa - khác icon cho ngày/đêm
                if (isDayTime) R.mipmap.sunny_rain else R.mipmap.nigh_rain
            }
            rainProbability > 20 -> {
                // Nhiều mây - khác icon cho ngày/đêm
                if (isDayTime) R.mipmap.sunny_cloudy else R.mipmap.night_cloudy
            }
            else -> {
                // Trời quang - khác icon cho ngày/đêm
                if (isDayTime) R.mipmap.sunny else R.mipmap.night
            }
        }
    }

    /**
     * Tính xác suất mưa từ giá trị rain
     */
    fun calculateRainProbability(rainValue: Double): Int {
        // Return percentage dựa trên thresholds khác nhau
        return when {
            rainValue < 0.005 -> 1   // Rất ít
            rainValue < 0.01 -> 2    // Ít
            rainValue < 0.05 -> 5    // Nhẹ
            rainValue < 0.1 -> 10    // Vừa phải
            rainValue < 0.25 -> 40   // Trung bình
            rainValue < 0.5 -> 80    // Cao
            else -> 100              // Rất cao
        }
    }

    /**
     * Tính điểm sương từ nhiệt độ và độ ẩm tương đối
     */
    private fun calculateDewPoint(t2m: Double, relativeHumidity: Double): Double {
        val a = 17.27  // Hằng số Magnus
        val b = 237.7  // Hằng số Magnus
        val rh = relativeHumidity / 100.0 // Convert percentage thành decimal
        // Tính alpha theo công thức Magnus
        val alpha = ((a * t2m) / (b + t2m)) + Math.log(rh)
        // Tính và return dew point
        return (b * alpha) / (a - alpha)
    }

    /**
     * Trả về giá trị lớn hơn giữa hai số
     */
    private fun max(a: Double, b: Double): Double = if (a > b) a else b

    /**
     * Tính thời gian mặt trời mọc và mặt trời lặn
     */
    @SuppressLint("NewApi") // Suppress warning về API level
    fun calculateSunTimes(
        latitude: Double, // Vĩ độ
        longitude: Double, // Kinh độ
        date: LocalDate = LocalDate.now(), // Ngày tính toán, default hôm nay
        timeZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh") // Múi giờ, default VN
    ): Pair<String, String> {
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // Formatter cho output
        val zenith = 90.83 // Góc thiên đỉnh chính thức (bao gồm khúc xạ khí quyển)
        val dayOfYear = date.dayOfYear // Ngày thứ mấy trong năm (1-365)
        val d = 2.0 * PI * (dayOfYear - 1) / 365.0 // Góc ngày trong năm (radians)

        // Tính độ nghiêng mặt trời (solar declination) theo công thức thiên văn
        val declination = 0.006918 - 0.399912 * cos(d) + 0.070257 * sin(d) -
                0.006758 * cos(2.0 * d) + 0.000907 * sin(2.0 * d) -
                0.002697 * cos(3.0 * d) + 0.00148 * sin(3.0 * d)

        val latRad = Math.toRadians(latitude) // Convert vĩ độ thành radians

        // Tính cosinus của góc giờ mặt trời
        val cosHourAngle = (cos(Math.toRadians(zenith)) -
                sin(latRad) * sin(declination)) / (cos(latRad) * cos(declination))

        // Default values nếu mặt trời không mọc/lặn (vùng cực)
        var sunriseStr = "Không có bình minh"
        var sunsetStr = "Không có hoàng hôn"

        // Kiểm tra xem có thể tính được thời gian mặt trời không
        if (cosHourAngle <= 1.0 && cosHourAngle >= -1.0) {
            val hourAngle = acos(cosHourAngle) // Tính góc giờ mặt trời

            // Tính hiệu chỉnh thời gian (equation of time)
            val eqTime = 0.123 * cos(d - 1.56) + 0.004 * cos(2.0 * d - 1.13)

            // Tính offset múi giờ
            val timeZoneOffset = timeZone.rules.getOffset(LocalDateTime.of(date, LocalTime.NOON))
                .totalSeconds / 3600.0
            val longitudeHour = longitude / 15.0 // Convert kinh độ thành giờ

            // Tính giờ địa phương cho mặt trời mọc
            var sunriseHour =
                (-Math.toDegrees(hourAngle) / 15.0) + eqTime - longitudeHour + timeZoneOffset
            sunriseHour = (sunriseHour + 24.0) % 24.0 // Normalize về 0-24h

            // Tính giờ địa phương cho mặt trời lặn
            var sunsetHour =
                (Math.toDegrees(hourAngle) / 15.0) + eqTime - longitudeHour + timeZoneOffset
            sunsetHour = (sunsetHour + 24.0) % 24.0 // Normalize về 0-24h

            // Convert decimal hours thành LocalTime
            val sunriseTime = LocalTime.of(
                sunriseHour.toInt(), // Giờ
                ((sunriseHour - sunriseHour.toInt()) * 60.0).roundToInt() // Phút
            )

            val sunsetTime = LocalTime.of(
                sunsetHour.toInt(), // Giờ
                ((sunsetHour - sunsetHour.toInt()) * 60.0).roundToInt() // Phút
            )

            // Format thành string
            sunriseStr = sunriseTime.format(formatter)
            sunsetStr = sunsetTime.format(formatter)
        }

        return Pair(sunriseStr, sunsetStr) // Return cặp thời gian mọc/lặn
    }

    /**
     * Làm tròn số theo quy tắc toán học
     */
    private fun roundNumber(value: Double): Int {
        val decimalPart = value - value.toInt() // Lấy phần thập phân
        return if (decimalPart < 0.5) {
            value.toInt()  // Làm tròn xuống nếu < 0.5
        } else {
            value.toInt() + 1  // Làm tròn lên nếu >= 0.5
        }
    }

    /**
     * Chuyển đổi date string thành tên thứ bằng tiếng Việt
     */
    @SuppressLint("NewApi") // Suppress warning về API level
    fun extractDayOfWeekInVietnamese(dateString: String): String {
        return try {
            // Tạo formatter với pattern yyyy-MM-dd
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            // Parse string thành LocalDate
            val date = LocalDate.parse(dateString, formatter)
            // Lấy day of week (1=Monday, 7=Sunday)
            val dayOfWeek = date.dayOfWeek.value

            // Array chứa tên các thứ bằng tiếng Việt
            val daysInVietnamese = arrayOf(
                "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
            )

            // Return tên thứ tương ứng (7=Sunday map to index 0, others shift by +1)
            if (dayOfWeek == 7) daysInVietnamese[0] else daysInVietnamese[dayOfWeek]
        } catch (e: Exception) {
            "Không xác định" // Return default nếu parse lỗi
        }
    }

    // =================================
    // PUBLIC METHODS - Các hàm public cho external control
    // =================================

    /**
     * Cập nhật trạng thái máy bơm từ bên ngoài Fragment
     * @param isOn true để bật, false để tắt máy bơm
     */
    fun updatePumpStatus(isOn: Boolean) {
        weatherDataViewModel.updatePumpStatus(isOn) // Delegate to ViewModel
    }

    /**
     * Cập nhật trạng thái mành che từ bên ngoài Fragment
     * @param isOn true để mở, false để đóng mành che
     */
    fun updateBlindStatus(isOn: Boolean) {
        weatherDataViewModel.updateBlindStatus(isOn) // Delegate to ViewModel
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
        timeUpdateJob?.cancel() // Cancel coroutine job để tránh leak
        _binding = null // Set binding null để tránh memory leak
    }
}