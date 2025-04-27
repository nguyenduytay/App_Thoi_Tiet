package com.example.weather2.View

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather2.Model.Entity.Weather24h
import com.example.weather2.Model.Fribase.FirebaseWeatherData
import com.example.weather2.Model.Entity.Weather24hData
import com.example.weather2.Model.Entity.Weather7d
import com.example.weather2.Model.Entity.Weather7dData
import com.example.weather2.Model.Fribase.FirebaseWeather24h
import com.example.weather2.Model.Fribase.FirebaseWeather7d
import com.example.weather2.R
import com.example.weather2.ViewModel.Weather24hViewModel
import com.example.weather2.ViewModel.Weather24hAdapter
import com.example.weather2.ViewModel.Weather7dAdapter
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
import java.util.Timer
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class WeatherFragment : Fragment() {
    private lateinit var bindingFragmentWeather: FragmentWeatherBinding
    private lateinit var bindingHourWeather: HourWeatherBinding
    private lateinit var bindingStatusSunWeather: StatusSunWeatherBinding
    private lateinit var bindingDayWeather: DayWeatherBinding
    private lateinit var weather24hViewmodel: Weather24hViewModel
    private lateinit var weather7dViewModel: Weather7dViewModel

    private var timeUpdateJob: Job? = null
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingFragmentWeather = FragmentWeatherBinding.inflate(inflater, container, false)
        bindingHourWeather =
            HourWeatherBinding.bind(bindingFragmentWeather.includeHourWeather.hourWeather)
        bindingStatusSunWeather =
            StatusSunWeatherBinding.bind(bindingFragmentWeather.includeStatusSunWeather.statusSunWeather)
        bindingDayWeather =
            DayWeatherBinding.bind(bindingFragmentWeather.includeDayWeather.dayWeather)
        weather24hViewmodel = ViewModelProvider(this)[Weather24hViewModel::class.java]
        weather7dViewModel=ViewModelProvider(this)[Weather7dViewModel::class.java]

        getDayEndTime()
        setupWeather24h()
        setupWeather7d()
        showDataEndCalculateWeather()
        bindingFragmentWeather.MotionLayout.background=context?.getDrawable(R.drawable.cloudy_bg)
        return bindingFragmentWeather.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeUpdateJob?.cancel()
    }

    //hàm cập nhật thời gian thực
    private fun getDayEndTime() {
        timeUpdateJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    bindingFragmentWeather.tvTime.text = getCurrentTime()
                    bindingFragmentWeather.tvDay.text = getCurrentDayInVN()
                    delay(1000)
                }
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
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    //kiểm tra kết nối mạng
    private fun isNetwork(): Boolean {
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // hàm chuyển đổi thời gian
    @SuppressLint("NewApi", "DefaultLocale")
    fun extractHourMinute(dateTimeString: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateTime = LocalDateTime.parse(dateTimeString, formatter)
        return "${dateTime.hour}:${String.format("%02d", dateTime.minute)}"
    }
    // Hàm lấy icon thời tiết dựa trên thời gian và khả năng mưa
    fun getWeatherIcon(timeStr: String, rainProbability: Int): Int {
        // Lấy giờ từ chuỗi thời gian
        val hour = timeStr.split(":")[0].toInt()

        // Xác định ban ngày hay ban đêm (6:00 - 18:00 là ban ngày)
        val isDayTime = hour in 6..18

        // Xác định icon dựa trên thời gian và khả năng mưa
        return when {
            //trường hớp nhiều mây (khả năng mưa > 60%)
            rainProbability > 60 -> {
                R.mipmap.rain
            }
            // Trường hợp mưa (40% < khả năng mưa <= 60% )
            rainProbability > 40 -> {
                if (isDayTime) R.mipmap.sunny_rain
                else R.mipmap.nigh_rain
            }

            // Trường hợp nhiều mây (20% < khả năng mưa <= 40%)
            rainProbability > 20 -> {
                if (isDayTime) R.mipmap.sunny_cloudy
                else R.mipmap.night_cloudy
            }

            // Trường hợp trời quang/ít mây (khả năng mưa <= 20%)
            else -> {
                if (isDayTime) R.mipmap.sunny
                else R.mipmap.night
            }
        }
    }

    //cập nhật dữ liệu dự báo và tính toán
    @SuppressLint("SetTextI18n")
    private fun showDataEndCalculateWeather() {
        //cập nhật thời gian mặt trời mọc và lặn
        // Vị trí Đà Nẵng
        val latitude = 16.0752
        val longitude = 108.1531
        val (sunriseStr, sunsetStr) = calculateSunTimes(latitude, longitude)

        bindingStatusSunWeather.tvStartSun.text = sunsetStr
        bindingStatusSunWeather.tvEndSun.text = sunriseStr

        // cập nhật max min nhiệt độ trong ngày

        FirebaseWeather24h.addListener { data ->
            val max = data.values.maxByOrNull { it.T2M }?.T2M
            val min = data.values.minByOrNull { it.T2M }?.T2M
            bindingFragmentWeather.tvTempDayMax1.text = max?.toInt().toString()
            bindingFragmentWeather.tvTempDayMin1.text = min?.toInt().toString()
        }
        FirebaseWeatherData.addListener { weatherData ->
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            bindingFragmentWeather.tvTempHourLive.text =
                weatherData.temperature?.let { roundNumber(it).toString().plus(" ℃") }
            bindingFragmentWeather.tvHumidyHourLive1.text =
                weatherData.humidity?.let { roundNumber(it).toString().plus(" %")}
            weatherData.rain?.let {
                getWeatherIcon(format.format(calendar.time).toString(),
                    it.toInt())
            }?.let { bindingFragmentWeather.ivWeatherHourLive.setImageResource(it) }
        }
    }
    // tính khả năng mưa dựa vào các thông số
    fun calculateRainProbability(
        prectotcorr: Double,     // PRECTOTCORR (mm/giờ)
        relativeHumidity: Double, // Độ ẩm tương đối (%)
        ps: Double,              // PS (kPa)
        t2m: Double,             // T2M (°C)
        allskyParTot: Double,    // ALLSKY_SFC_PAR_TOT (W/m²)
        elevation: Double = 0.0  // Độ cao (m)
    ): Int {
        // Nếu đang mưa, xác suất mưa rất cao nhưng không nhất thiết là 100%
        // Mưa nhỏ (< 0.5mm/h) có thể là mưa phùn hoặc khu vực xung quanh
        if (prectotcorr > 0) {
            return when {
                prectotcorr < 0.1 -> 80
                prectotcorr < 0.5 -> 90
                else -> 100  // Mưa rõ ràng
            }
        }

        // Tính điểm chỉ số mưa
        var rainScore = 0.0

        // 1. Đánh giá độ ẩm tương đối (0-45 điểm)
        val humidityScore = when {
            relativeHumidity < 40 -> 0.0  // Quá khô, không mưa
            relativeHumidity < 60 -> (relativeHumidity - 40) * 0.75  // 0-15 điểm
            relativeHumidity < 80 -> 15 + (relativeHumidity - 60) * 1.0  // 15-35 điểm
            else -> 35 + (relativeHumidity - 80) * 0.5  // 35-45 điểm (tối đa)
        }

        // 2. Đánh giá áp suất (0-25 điểm)
        // Điều chỉnh áp suất theo độ cao (giảm khoảng 1.2kPa/100m)
        val elevationAdjustment = elevation * 0.012
        val adjustedPressureThreshold = 101.3 - elevationAdjustment  // Ngưỡng áp suất chuẩn (kPa)

        val pressureScore = when {
            ps < (adjustedPressureThreshold - 1.5) -> 25.0  // Áp suất rất thấp
            ps < adjustedPressureThreshold ->
                25 - ((ps - (adjustedPressureThreshold - 1.5)) * 25 / 1.5)  // 0-25 điểm
            else -> max(
                0.0,
                10 - ((ps - adjustedPressureThreshold) * 10)
            )  // Áp suất cao, ít khả năng mưa
        }

        // 3. Đánh giá nhiệt độ và độ ẩm kết hợp - Tính điểm sương chính xác (0-20 điểm)
        val dewPoint = calculateDewPoint(t2m, relativeHumidity)
        val dewPointDelta = t2m - dewPoint
        val dewPointScore = when {
            dewPointDelta < 1.5 -> 20.0  // Rất gần điểm sương
            dewPointDelta < 3.0 -> 15.0  // Khá gần điểm sương
            dewPointDelta < 5.0 -> 10.0  // Gần điểm sương
            dewPointDelta < 8.0 -> 5.0   // Hơi xa điểm sương
            else -> 0.0               // Xa điểm sương
        }

        // 4. Đánh giá bức xạ mặt trời (0-10 điểm)
        // Nếu bức xạ mặt trời thấp, thường ít có mây hoặc đã muộn/sớm trong ngày
        val maxSolarRadiation = 1000.0 // Giá trị tối đa có thể của PAR
        val solarScore = when {
            allskyParTot < 100 -> 0.0    // Bức xạ rất thấp (đêm/sáng sớm/chiều muộn)
            allskyParTot < 300 -> 5.0    // Bức xạ thấp (nhiều mây)
            allskyParTot < 500 -> 10.0   // Bức xạ trung bình (có mây)
            allskyParTot < 700 -> 5.0    // Bức xạ cao (ít mây)
            else -> 0.0                   // Bức xạ rất cao (nắng)
        }

        // Tổng hợp điểm
        rainScore = humidityScore + pressureScore + dewPointScore + solarScore

        // Giới hạn kết quả
        return rainScore.toInt().coerceIn(0, 99)  // Tối đa 99% vì 100% chỉ khi đang mưa
    }

    //Tính điểm sương từ nhiệt độ và độ ẩm tương đối
    private fun calculateDewPoint(t2m: Double, relativeHumidity: Double): Double {
        val a = 17.27
        val b = 237.7
        val rh = relativeHumidity / 100.0
        val alpha = ((a * t2m) / (b + t2m)) + Math.log(rh)
        return (b * alpha) / (a - alpha)
    }
    //Trả về giá trị lớn hơn giữa hai số
    private fun max(a: Double, b: Double): Double = if (a > b) a else b

    //hàm tính thời gian mặt trời mọc và mặt trời lặn
    @SuppressLint("NewApi")
    fun calculateSunTimes(
        latitude: Double,
        longitude: Double,
        @SuppressLint("NewApi") date: LocalDate = LocalDate.now(),
        @SuppressLint("NewApi") timeZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    ): Pair<String, String> {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        // Góc thiên đỉnh chính thức (bao gồm hiệu ứng khúc xạ khí quyển)
        val zenith = 90.83

        // Tính ngày trong năm (1-365)
        val dayOfYear = date.dayOfYear

        // Tính toán các thông số thiên văn
        val d = 2.0 * PI * (dayOfYear - 1) / 365.0

        // Tính độ xích đạo mặt trời (δ)
        val declination = 0.006918 - 0.399912 * cos(d) + 0.070257 * sin(d) -
                0.006758 * cos(2.0 * d) + 0.000907 * sin(2.0 * d) -
                0.002697 * cos(3.0 * d) + 0.00148 * sin(3.0 * d)

        // Chuyển đổi vĩ độ từ độ sang radian
        val latRad = Math.toRadians(latitude)

        // Tính cosinus của góc giờ mặt trời
        val cosHourAngle = (cos(Math.toRadians(zenith)) -
                sin(latRad) * sin(declination)) / (cos(latRad) * cos(declination))

        // Xử lý trường hợp mặt trời không mọc hoặc không lặn
        var sunriseStr = "Không có bình minh"
        var sunsetStr = "Không có hoàng hôn"

        if (cosHourAngle <= 1.0 && cosHourAngle >= -1.0) {
            // Tính góc giờ mặt trời (theo radian)
            val hourAngle = acos(cosHourAngle)

            // Tính hiệu chỉnh thời gian (equation of time)
            val eqTime = 0.123 * cos(d - 1.56) + 0.004 * cos(2.0 * d - 1.13)

            // Hiệu chỉnh kinh độ và múi giờ
            val timeZoneOffset = timeZone.rules.getOffset(LocalDateTime.of(date, LocalTime.NOON))
                .totalSeconds / 3600.0
            val longitudeHour = longitude / 15.0

            // Tính giờ địa phương cho mặt trời mọc
            var sunriseHour =
                (-Math.toDegrees(hourAngle) / 15.0) + eqTime - longitudeHour + timeZoneOffset
            sunriseHour = (sunriseHour + 24.0) % 24.0

            // Tính giờ địa phương cho mặt trời lặn
            var sunsetHour =
                (Math.toDegrees(hourAngle) / 15.0) + eqTime - longitudeHour + timeZoneOffset
            sunsetHour = (sunsetHour + 24.0) % 24.0

            // Chuyển đổi thành LocalTime
            val sunriseTime = LocalTime.of(
                sunriseHour.toInt(),
                ((sunriseHour - sunriseHour.toInt()) * 60.0).roundToInt()
            )

            val sunsetTime = LocalTime.of(
                sunsetHour.toInt(),
                ((sunsetHour - sunsetHour.toInt()) * 60.0).roundToInt()
            )

            // Định dạng thời gian thành chuỗi
            sunriseStr = sunriseTime.format(formatter)
            sunsetStr = sunsetTime.format(formatter)
        }

        return Pair(sunriseStr, sunsetStr)
    }

    //Giá trị cần làm tròn
    private fun roundNumber(value: Double): Int {
        val decimalPart = value - value.toInt()
        return if (decimalPart < 0.5) {
            value.toInt()  // Làm tròn xuống
        } else {
            value.toInt() + 1  // Làm tròn lên
        }
    }
    // cập nhật thời tiết theo giờ
    @SuppressLint("DefaultLocale")
    private fun setupWeather24h() {
        //kiểm tra kết nối mạng
        if (isNetwork()) {
            // Lắng nghe sự thay đổi dữ liệu từ Firebase
            FirebaseWeather24h.addListener { weatherMap ->
                // Chuyển đổi dữ liệu từ Firebase thành danh sách mà RecyclerView cần
                val weatherList = weatherMap.entries.map { entry ->
                    // Tạo đối tượng WeatherData từ dữ liệu Firebase
                    val time = extractHourMinute(entry.key)
                    val weatherData = entry.value
                    val  rainProbability = calculateRainProbability(
                        weatherData.PRECTOTCORR,
                        weatherData.QV2M,
                        weatherData.PS,
                        weatherData.T2M,
                        weatherData.ALLSKY_SFC_PAR_TOT,
                        10.0
                    )
                    Weather24hData(
                        time = time,
                        temperature = roundNumber(weatherData.T2M),  // Làm tròn nhiệt độ
                        rainProbability = rainProbability,
                        icon = getWeatherIcon(time, rainProbability)  // Hàm lấy icon thời tiết
                    )
                }
                val weatherListRoomData = weatherMap.entries.map { entry ->
                    Weather24h(
                        0,
                        entry.key,
                        entry.value.ALLSKY_SFC_PAR_TOT,
                        entry.value.PRECTOTCORR,
                        entry.value.PS,
                        entry.value.QV2M,
                        entry.value.T2M
                    )

                }
                // Cập nhật RecyclerView với dữ liệu mới
                bindingHourWeather.recyclerViewWeatherHour.apply {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = Weather24hAdapter(weatherList)
                }
                weather24hViewmodel.refreshWeather24hData(weatherListRoomData)
            }
        } else {
            lifecycleScope.launch {
                val weatherList = weather24hViewmodel.getAllWeather24h()
                val weather24hData = weatherList.map { entity ->
                    val time = extractHourMinute(entity.time)
                    val rainProbability = calculateRainProbability(
                        entity.PRECTOTCORR,
                        entity.QV2M,
                        entity.PS,
                        entity.T2M,
                        entity.ALLSKY_SFC_PAR_TOT,
                        10.0
                    )
                    Weather24hData(
                        time = time,
                        temperature = roundNumber(entity.T2M),
                        rainProbability = rainProbability,
                        icon = getWeatherIcon(time, rainProbability)
                    )
                }
                bindingHourWeather.recyclerViewWeatherHour.apply {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = Weather24hAdapter(weather24hData)
                }
            }
        }
    }

    /**
     * Chuyển đổi chuỗi ngày tháng thành tên thứ trong tuần bằng tiếng Việt
     * @param dateString Chuỗi ngày tháng định dạng "yyyy-MM-dd"
     * @return Tên thứ trong tuần bằng tiếng Việt (Ví dụ: "Thứ Hai", "Chủ nhật")
     */
    @SuppressLint("NewApi")
    fun extractDayOfWeekInVietnamese(dateString: String): String {
        // Định dạng formatter phù hợp với chuỗi đầu vào
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // Parse chuỗi ngày tháng thành LocalDate
        val date = LocalDate.parse(dateString, formatter)

        // Lấy giá trị thứ trong tuần (1: Thứ Hai, 7: Chủ nhật)
        val dayOfWeek = date.dayOfWeek.value

        // Danh sách tên thứ trong tuần bằng tiếng Việt
        val daysInVietnamese = arrayOf(
            "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
        )

        // Trả về tên thứ tương ứng (cần điều chỉnh index vì dayOfWeek.value bắt đầu từ 1, không phải 0)
        return if (dayOfWeek == 7) daysInVietnamese[0] else daysInVietnamese[dayOfWeek]
    }

    //cập nhật thời tiết 7 ngày
    private fun setupWeather7d() {
        if(isNetwork()) {
            // Lắng nghe sự thay đổi dữ liệu từ Firebase
            FirebaseWeather7d.addListener { weatherMap ->
                // Chuyển đổi dữ liệu từ Firebase thành danh sách mà RecyclerView cần
                val weatherList = weatherMap.entries.map { entry ->
                    // Tạo đối tượng WeatherData từ dữ liệu Firebase
                    val time = extractDayOfWeekInVietnamese(entry.key)
                    val weatherData = entry.value
                    val rainProbability_max = calculateRainProbability(
                        weatherData.PRECTOTCORR,
                        weatherData.QV2M,
                        weatherData.PS,
                        weatherData.T2M_max,
                        weatherData.ALLSKY_SFC_PAR_TOT,
                        10.0
                    )
                    val rainProbability_min = calculateRainProbability(
                        weatherData.PRECTOTCORR,
                        weatherData.QV2M,
                        weatherData.PS,
                        weatherData.T2M_min,
                        weatherData.ALLSKY_SFC_PAR_TOT,
                        10.0
                    )
                    Weather7dData(
                        time = time,
                        temperatureMax = roundNumber(weatherData.T2M_max),
                        temperatureMin = roundNumber(weatherData.T2M_min),
                        rainProbability = rainProbability_min,
                        icon_morning = getWeatherIcon("6:00", rainProbability_max),
                        icon_evening = getWeatherIcon("22:00", rainProbability_min)
                    )
                }
                val weatherListRoomData = weatherMap.entries.map { entry ->
                    Weather7d(
                        0,
                        entry.key,
                        entry.value.ALLSKY_SFC_PAR_TOT,
                        entry.value.PRECTOTCORR,
                        entry.value.PS,
                        entry.value.QV2M,
                        entry.value.T2M_max,
                        entry.value.T2M_min
                    )
                }
                // Cập nhật RecyclerView với dữ liệu mới
                bindingDayWeather.rvDayWeather.apply {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    adapter = Weather7dAdapter(weatherList)
                }
                weather7dViewModel.refreshWeather7dData(weatherListRoomData)
            }
        } else {
            lifecycleScope.launch {
                val weatherList = weather7dViewModel.getAllWeather7d()
                val weather7dData = weatherList.map { entity ->
                    val time = extractDayOfWeekInVietnamese(entity.time)
                    val rainProbability_max = calculateRainProbability(
                        entity.PRECTOTCORR,
                        entity.QV2M,
                        entity.PS,
                        entity.T2M_MAX,
                        entity.ALLSKY_SFC_PAR_TOT,
                        10.0
                    )
                    val rainProbability_min = calculateRainProbability(
                        entity.PRECTOTCORR,
                        entity.QV2M,
                        entity.PS,
                        entity.T2M_MIN,
                        entity.ALLSKY_SFC_PAR_TOT,
                        10.0
                    )
                    Weather7dData(
                        time = time,
                        temperatureMax = roundNumber(entity.T2M_MAX),
                        temperatureMin = roundNumber(entity.T2M_MIN),
                        rainProbability = rainProbability_min,
                        icon_morning = getWeatherIcon("6:00", rainProbability_min),
                        icon_evening = getWeatherIcon("22:00", rainProbability_min)
                    )
                }
                bindingDayWeather.rvDayWeather.apply {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = Weather7dAdapter(weather7dData)
                }
            }
        }
    }
}