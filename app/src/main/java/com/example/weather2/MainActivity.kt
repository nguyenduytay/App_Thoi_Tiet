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
