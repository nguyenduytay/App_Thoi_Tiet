package com.example.weather2

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.weather2.Database.AppDatabase
import com.example.weather2.View.Notification.FCMTokenManager
import com.example.weather2.ViewModel.DepthPageTransformer
import com.example.weather2.ViewModel.MyViewpager2Adapter
import com.example.weather2.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MyViewpager2Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kiểm tra quyền đặt báo thức chính xác trên Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }

        // Xử lý khi mở từ thông báo
        if (intent?.getBooleanExtra("from_notification", false) == true) {
            // Có thể thực hiện hành động đặc biệt khi mở từ thông báo
            Toast.makeText(this, "Ứng dụng được mở từ thông báo", Toast.LENGTH_SHORT).show()
        }

        //sự kiện chuyển đổi fragment
        setUpViewpager2()

        // Cài đặt UncaughtExceptionHandler để bắt lỗi toàn cục
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppError", "Lỗi toàn cục: ${throwable.message}", throwable)
            System.exit(1)
        }
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
