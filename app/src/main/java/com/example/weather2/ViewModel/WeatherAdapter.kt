package com.example.weather2.ViewModel

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.example.weather2.Model.WeatherData
import com.example.weather2.databinding.ItemWeatherBinding

class WeatherAdapter(private val weatherList: List<WeatherData>) : RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder>() {

    private val points = mutableListOf<Pair<Float, Float>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val binding = ItemWeatherBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return WeatherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        val weatherData = weatherList[position]
        with(holder.binding) {
            tvTime.text = weatherData.time
            tvTemperature.text = "${weatherData.temperature}°"
            tvHumidity.text = "${weatherData.humidity}%"

            // Lấy nhiệt độ tối đa và tối thiểu từ danh sách
            val maxTemp = weatherList.maxOf { it.temperature }
            val minTemp = weatherList.minOf { it.temperature }
            val value = weatherList[position].temperature
            var valueBefore:Int=0
            var valueAfter:Int=0
            if(position-1 >= 0)
            {
                valueBefore=weatherList[position-1].temperature
            }
            if(position+1<weatherList.size)
            {
                valueAfter=weatherList[position+1].temperature
            }
            WeatherItemView.setData(value,valueAfter,valueBefore,maxTemp,minTemp)
        }

    }

    override fun getItemCount() = weatherList.size

    class WeatherViewHolder(val binding: ItemWeatherBinding) : RecyclerView.ViewHolder(binding.root)
}