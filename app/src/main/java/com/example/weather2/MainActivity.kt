package com.example.weather2

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather2.Model.WeatherData
import com.example.weather2.Model.testWeather
import com.example.weather2.ViewModel.WeatherAdapter
import com.example.weather2.databinding.ActivityMainBinding
import com.example.weather2.databinding.HourWeatherBinding
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingHourWeather: HourWeatherBinding
    private var lastScrollTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindingHourWeather = HourWeatherBinding.bind(binding.includeHourWeather.hourWeather)
        setupRecyclerView()
        //cập nhật thời gian liên tục
        getDayEndTime()
//         đẩy dữ liệu lên firestore database
//        val db = FirebaseFirestore.getInstance()
//        // Thêm dữ liệu vào collection "weather_data"
//        val weather=testWeather("weather123",30,70,35,25)
//        db.collection("weather_data").document(weather.id ?: "default_id")
//            .set(weather)
//            .addOnSuccessListener {
//                println("Dữ liệu đã được đẩy lên Firebase thành công!")
//            }
//            .addOnFailureListener { e ->
//                println("Lỗi khi đẩy dữ liệu: ${e.message}")
//            }


        // cập nhật dữ liệu liên tục từ firestore database
//        val db = FirebaseFirestore.getInstance()
//        val docRef = db.collection("weather_data").document("weather123")
//
//        // Lắng nghe thay đổi trong document "weather123"
//        docRef.addSnapshotListener { snapshot, error ->
//            if (error != null) {
//                Log.e("Firestore", "Lỗi khi lắng nghe Firestore: ${error.message}")
//                return@addSnapshotListener
//            }
//
//            if (snapshot != null && snapshot.exists()) {
//                val id = snapshot.getString("id") ?: "N/A"
//                val temperature = snapshot.getLong("temperature")?.toInt() ?: 0
//                val humidity = snapshot.getLong("humidity")?.toInt() ?: 0
//                val tempMax = snapshot.getLong("temperatureMax")?.toInt() ?: 0
//                val tempMin = snapshot.getLong("temperatureMin")?.toInt() ?: 0
//
//                binding.tvTempHourLive.text=temperature.toString().plus("°")
//                binding.tvHumidyHourLive1.text=humidity.toString().plus("%")
//                binding.tvHumidyHourLive2.text=humidity.toString().plus("%")
//                binding.tvTempDayMax1.text=tempMax.toString().plus("°")
//                binding.tvTempDayMax2.text=tempMax.toString().plus("°")
//                binding.tvTempDayMin1.text=tempMin.toString().plus("°")
//                binding.tvTempDayMin2.text=tempMin.toString().plus("°")
//
//                Log.d("Firestore", "🔥 Cập nhật dữ liệu: ID=$id, Temp=$temperature, Humidity=$humidity")
//
//            } else {
//                Log.d("Firestore", "Document không tồn tại!")
//            }
//        }


        //cập nhật dữ liệu liên tục từ realtime database
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("weather_data")


        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
                    val humidity = snapshot.child("humidity").getValue(Double::class.java) ?: 0.0
                    val pressure = snapshot.child("pressure").getValue(Double::class.java) ?: 0.0
                    val light = snapshot.child("light").getValue(Int::class.java) ?: 0
                    val rain = snapshot.child("rain").getValue(Int::class.java) ?: 0

                    // Cập nhật dữ liệu lên giao diện
                    binding.tvTempHourLive.text = "${temperature}°"
                    binding.tvHumidyHourLive1.text = "${humidity}%"
                    binding.tvHumidyHourLive2.text = "${humidity}%"
                    binding.tvTempDayMax1.text = "${temperature + 2}°"  // Ví dụ dữ liệu giả lập
                    binding.tvTempDayMax2.text = "${temperature + 2}°"
                    binding.tvTempDayMin1.text = "${temperature - 2}°"
                    binding.tvTempDayMin2.text = "${temperature - 2}°"

                    Log.d("Firebase", "Dữ liệu cập nhật: Temp=$temperature, Humidity=$humidity, Pressure=$pressure, Light=$light, Rain=$rain")
                } else {
                    Log.d("Firebase", "Không có dữ liệu trong Realtime Database!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Lỗi khi đọc dữ liệu: ${error.message}")
            }
        })

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
    private fun toggleVisibility(visible1: Int, visible2: Int) {
        binding.ll1.visibility = visible1
        binding.ll2.visibility = visible1
        binding.ll3.visibility = visible2
        binding.ll4.visibility = visible2
        binding.ll5.visibility = visible2
    }
//hàm cập nhật thời gian liên tục
    private fun getDayEndTime()
    {
        lifecycleScope.launch {
            while(isActive)
            {
                binding.tvTime.text=getCurrentTime()
                binding.tvDay.text=getCurrentDayInVN()
                delay(10)
            }
        }
    }
// hàm lấy thứ trong tuần bằng tiếng việt
    private fun getCurrentDayInVN() : String
    {
    val calendar=Calendar.getInstance()
        val daysOfWeek= arrayOf(
            "Chủ nhật", "Thứ Hai", "Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy"
        )
        return daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK)-1]
    }
    //Hàm lấy thời gian
    private fun getCurrentTime() : String
    {
        val sdf=SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
