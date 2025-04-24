package com.example.weather2.ViewModel

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weather2.Model.Entity.Weather7dData
import com.example.weather2.databinding.ItemDayWeatherBinding

class Weather7dAdapter(private val weatherList: List<Weather7dData>) : RecyclerView.Adapter<Weather7dAdapter.WeatherViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val binding = ItemDayWeatherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WeatherViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        val weatherData = weatherList[position]
        with(holder.binding) {
            tvDay.text = weatherData.time
            tvTempMax.text = "${weatherData.temperatureMax}°"
            tvTempMin.text = "${weatherData.temperatureMin}°"
            tvRain.text = "${weatherData.rainProbability}%"
            // Thiết lập icon thời tiết cho buổi sáng và tối
            ivMorning.setImageResource(weatherData.icon_morning)
            ivEverning.setImageResource(weatherData.icon_evening)
        }
    }

    override fun getItemCount() = weatherList.size

    class WeatherViewHolder(val binding: ItemDayWeatherBinding) : RecyclerView.ViewHolder(binding.root)
}