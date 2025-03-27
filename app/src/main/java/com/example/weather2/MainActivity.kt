package com.example.weather2

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.weather2.Model.WeatherData
import com.example.weather2.Model.testWeather
import com.example.weather2.ViewModel.DepthPageTransformer
import com.example.weather2.ViewModel.MyViewpager2Adapter
import com.example.weather2.ViewModel.WeatherAdapter
import com.example.weather2.databinding.ActivityMainBinding
import com.example.weather2.databinding.HourWeatherBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
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
    private lateinit var adapter: MyViewpager2Adapter

    private var lastScrollTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //sự kiện chuyển đổi fragment
        setUpViewpager2()


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


//        //cập nhật dữ liệu liên tục từ realtime database
//        val database = FirebaseDatabase.getInstance()
//        val myRef = database.getReference("weather_data")

//        myRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()) {
//                    val temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
//                    val humidity = snapshot.child("humidity").getValue(Double::class.java) ?: 0.0
//                    val pressure = snapshot.child("pressure").getValue(Double::class.java) ?: 0.0
//                    val light = snapshot.child("light").getValue(Int::class.java) ?: 0
//                    val rain = snapshot.child("rain").getValue(Int::class.java) ?: 0
//
//                    // Cập nhật dữ liệu lên giao diện
//                    binding.tvTempHourLive.text = "${temperature}°"
//                    binding.tvHumidyHourLive1.text = "${humidity}%"
//                    binding.tvHumidyHourLive2.text = "${humidity}%"
//                    binding.tvTempDayMax1.text = "${temperature + 2}°"  // Ví dụ dữ liệu giả lập
//                    binding.tvTempDayMax2.text = "${temperature + 2}°"
//                    binding.tvTempDayMin1.text = "${temperature - 2}°"
//                    binding.tvTempDayMin2.text = "${temperature - 2}°"
//
//                    Log.d("Firebase", "Dữ liệu cập nhật: Temp=$temperature, Humidity=$humidity, Pressure=$pressure, Light=$light, Rain=$rain")
//                } else {
//                    Log.d("Firebase", "Không có dữ liệu trong Realtime Database!")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("Firebase", "Lỗi khi đọc dữ liệu: ${error.message}")
//            }
//        })
//
    }

    //sự kiện chuyển đổi fragment
    private fun setUpViewpager2() {
        // Chọn menu System (ở giữa) ngay từ đầu
        binding.mainMenuBottomNavigation.selectedItemId = R.id.menu_system

        // Khởi tạo adapter
        adapter = MyViewpager2Adapter(this)
        binding.mainBodyViewPager2.adapter = adapter

        // Đặt ViewPager2 ở trang SystemFragment (index 1)
        binding.mainBodyViewPager2.currentItem = 1

        // Hiệu ứng chuyển đổi trang
        binding.mainBodyViewPager2.setPageTransformer(DepthPageTransformer())

        // Lắng nghe sự kiện thay đổi trang
        binding.mainBodyViewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.mainMenuBottomNavigation.selectedItemId = when (position) {
                    0 -> R.id.menu_weather
                    1 -> R.id.menu_system
                    2 -> R.id.menu_setting
                    else -> R.id.menu_system
                }
            }
        })

        // Xử lý khi người dùng chọn một mục trên BottomNavigationView
        binding.mainMenuBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_weather -> binding.mainBodyViewPager2.currentItem = 0
                R.id.menu_system -> binding.mainBodyViewPager2.currentItem = 1
                R.id.menu_setting -> binding.mainBodyViewPager2.currentItem = 2
            }
            true
        }
    }

}
