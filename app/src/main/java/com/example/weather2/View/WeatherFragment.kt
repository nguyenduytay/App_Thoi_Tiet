package com.example.weather2.View

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather2.Model.Fribase.FirebaseWeatherData
import com.example.weather2.Model.Entity.WeatherData
import com.example.weather2.ViewModel.WeatherAdapter
import com.example.weather2.databinding.DayWeatherBinding
import com.example.weather2.databinding.FragmentWeatherBinding
import com.example.weather2.databinding.HourWeatherBinding
import com.example.weather2.databinding.StatusSunWeatherBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeatherFragment : Fragment() {
    private lateinit var bindingFragmentWeather: FragmentWeatherBinding
    private lateinit var bindingHourWeather: HourWeatherBinding
    private lateinit var bindingStatusSunWeather: StatusSunWeatherBinding
    private lateinit var bindingDayWeather: DayWeatherBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingFragmentWeather=FragmentWeatherBinding.inflate(inflater,container,false)
        bindingHourWeather = HourWeatherBinding.bind(bindingFragmentWeather.includeHourWeather.hourWeather)
        bindingStatusSunWeather=StatusSunWeatherBinding.bind(bindingFragmentWeather.includeStatusSunWeather.statusSunWeather)
        bindingDayWeather=DayWeatherBinding.bind(bindingFragmentWeather.includeDayWeather.dayWeather)
        getDayEndTime()
        setupRecyclerView()
        updateWeather()
        return bindingFragmentWeather.root
    }
    //hàm cập nhật thời gian thực
    private fun getDayEndTime()
    {
        lifecycleScope.launch {
            while(isActive)
            {
                bindingFragmentWeather.tvTime.text=getCurrentTime()
                bindingFragmentWeather.tvDay.text=getCurrentDayInVN()
                delay(10)
            }
        }
    }
    // hàm lấy thứ trong tuần bằng tiếng việt
    private fun getCurrentDayInVN() : String
    {
        val calendar= Calendar.getInstance()
        val daysOfWeek= arrayOf(
            "Chủ nhật", "Thứ Hai", "Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy"
        )
        return daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK)-1]
    }
    //hàm cập nhật thời gian liên tục
    private fun getCurrentTime() : String
    {
        val sdf= SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
    private fun setupRecyclerView() {
        val weatherList = listOf(
            WeatherData("10:00", 23, 64, "10d"),
            WeatherData("11:00", 24, 66, "10d"),
            WeatherData("12:00", 25, 73, "10d"),
            WeatherData("13:00", 23, 74, "10d"),
            WeatherData("14:00", 26, 67, "10d"),
            WeatherData("15:00", 23, 68, "10d"),
            WeatherData("10:00", 23, 64, "10d"),
            WeatherData("11:00", 24, 66, "10d"),
            WeatherData("12:00", 25, 73, "10d"),
            WeatherData("13:00", 23, 74, "10d"),
            WeatherData("14:00", 26, 67, "10d"),
            WeatherData("15:00", 23, 68, "10d"),
            WeatherData("10:00", 23, 64, "10d"),
            WeatherData("11:00", 24, 66, "10d"),
            WeatherData("12:00", 25, 73, "10d"),
            WeatherData("13:00", 23, 74, "10d"),
            WeatherData("14:00", 26, 67, "10d"),
            WeatherData("15:00", 23, 68, "10d")
        )
        bindingHourWeather.recyclerViewWeatherHour.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = WeatherAdapter(weatherList)
        }
    }
    //sự kiện cập nhật thời tiết thay đổi liên tục
    private fun updateWeather()
    {
        FirebaseWeatherData.addListener {
            weatherData ->
            bindingFragmentWeather.tvTempHourLive.text=weatherData.temperature.toString().plus(" ℃")
            bindingFragmentWeather.tvHumidyHourLive1.text=weatherData.humidity.toString().plus(" %")
        }
    }
}