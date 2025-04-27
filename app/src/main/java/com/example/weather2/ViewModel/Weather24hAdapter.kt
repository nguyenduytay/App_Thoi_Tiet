package com.example.weather2.ViewModel

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.example.weather2.Model.Entity.Weather24hData
import com.example.weather2.databinding.ItemWeatherBinding

class Weather24hAdapter(private val weatherList: List<Weather24hData>) : RecyclerView.Adapter<Weather24hAdapter.WeatherViewHolder>() {

    private val points = mutableListOf<Pair<Float, Float>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val binding = ItemWeatherBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return WeatherViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        val weatherData = weatherList[position]
        with(holder.binding) {
            tvTime.text = weatherData.time
            tvTemperature.text = "${weatherData.temperature}°"
            tvHumidity.text = "${weatherData.rainProbability}%"
            ivWeatherIcon.setImageResource(weatherData.icon)
            // Lấy nhiệt độ tối đa và tối thiểu từ danh sách
            val maxTemp = weatherList.maxOf { it.temperature.toInt() }
            val minTemp = weatherList.minOf { it.temperature.toInt() }
            val value = weatherList[position].temperature.toInt()
            var valueBefore:Int=0
            var valueAfter:Int=0
            if(position-1 >= 0)
            {
                valueBefore=weatherList[position-1].temperature.toInt()
            }
            if(position+1<weatherList.size)
            {
                valueAfter=weatherList[position+1].temperature.toInt()
            }
            WeatherItemLineTempeView.setData(value,valueAfter,valueBefore,maxTemp,minTemp)
        }

    }

    override fun getItemCount() = weatherList.size

    class WeatherViewHolder(val binding: ItemWeatherBinding) : RecyclerView.ViewHolder(binding.root)
}