package com.example.weather2.View

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.weather2.R
import com.example.weather2.Ui.Animations.DepthPageTransformer
import com.example.weather2.Adapter.MyViewpager2Adapter
import com.example.weather2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MyViewpager2Adapter

    // ✅ THÊM: Flag để tránh loop giữa ViewPager và Menu
    private var isUpdatingFromCode = false

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
            Toast.makeText(this, "Ứng dụng được mở từ thông báo", Toast.LENGTH_SHORT).show()
        }

        // QUAN TRỌNG: Disable tint hoàn toàn
        binding.mainMenuBottomNavigation.itemIconTintList = null
        binding.mainMenuBottomNavigation.itemTextColor = null

        // Force refresh menu để áp dụng
        binding.mainMenuBottomNavigation.menu.clear()
        binding.mainMenuBottomNavigation.inflateMenu(R.menu.menu_bottom_navigation)

        // ✅ SỬA: Setup ViewPager2 sau khi menu đã ready
        setUpViewpager2()

        // Cài đặt UncaughtExceptionHandler để bắt lỗi toàn cục
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppError", "Lỗi toàn cục: ${throwable.message}", throwable)
            System.exit(1)
        }
    }

    // ✅ SỬA: Thứ tự setup đúng
    private fun setUpViewpager2() {
        // Khởi tạo adapter trước
        adapter = MyViewpager2Adapter(this)
        binding.mainBodyViewPager2.adapter = adapter

        // Hiệu ứng chuyển đổi trang
        binding.mainBodyViewPager2.setPageTransformer(DepthPageTransformer())

        // ✅ QUAN TRỌNG: Setup listeners trước khi set currentItem
        setupPageChangeCallback()
        setupBottomNavigationListener()

        // ✅ SỬA: Set cả ViewPager và Menu cùng lúc với flag
        setCurrentPage(1) // Mặc định là trang System (index 1)
    }

    // ✅ TÁCH HÀM: Setup callback cho ViewPager
    private fun setupPageChangeCallback() {
        binding.mainBodyViewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // ✅ CHỈ update menu khi không phải từ code
                if (!isUpdatingFromCode) {
                    Log.d("MainActivity", "ViewPager changed to position: $position")
                    updateMenuSelection(position)
                }
            }
        })
    }

    // ✅ TÁCH HÀM: Setup listener cho BottomNavigation
    private fun setupBottomNavigationListener() {
        binding.mainMenuBottomNavigation.setOnItemSelectedListener { item ->
            if (!isUpdatingFromCode) {
                val newPosition = when (item.itemId) {
                    R.id.menu_weather -> 0
                    R.id.menu_system -> 1
                    R.id.menu_chart -> 2
                    R.id.menu_setting -> 3
                    else -> 1 // Default to System
                }
                Log.d("MainActivity", "Menu clicked, moving to position: $newPosition")
                setCurrentPage(newPosition)
            }
            true
        }
    }

    //  Set page đồng bộ ViewPager + Menu
    private fun setCurrentPage(position: Int) {
        isUpdatingFromCode = true

        try {
            // Update ViewPager
            binding.mainBodyViewPager2.currentItem = position

            // Update Menu
            updateMenuSelection(position)

            Log.d("MainActivity", "Successfully set page to: $position")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting page: ${e.message}")
        } finally {
            // ✅ Reset flag sau một delay ngắn để đảm bảo animation hoàn thành
            binding.mainBodyViewPager2.post {
                isUpdatingFromCode = false
            }
        }
    }

    // ✅ SỬA: Update menu selection VỚI LOGIC ẨN/HIỆN
    private fun updateMenuSelection(position: Int) {
        val menuItemId = when (position) {
            0 -> R.id.menu_weather
            1 -> R.id.menu_system
            2 -> R.id.menu_chart
            3 -> R.id.menu_setting
            else -> R.id.menu_system
        }

        // ✅ LOGIC ẨN/HIỆN MENU
        if (position == 0) {
            // Trang 0 (Weather) → Ẩn menu
            hideBottomNav()
        } else {
            // Trang khác → Hiện menu
            showBottomNav()
            // Chỉ update selection khi menu đang hiện
            if (binding.mainMenuBottomNavigation.selectedItemId != menuItemId) {
                binding.mainMenuBottomNavigation.selectedItemId = menuItemId
                Log.d("MainActivity", "Menu selection updated to: $menuItemId")
            }
        }
    }

    // ✅ HÀM MỚI: Ẩn menu với animation mượt
    private fun hideBottomNav() {
        if (binding.mainMenuBottomNavigation.visibility == View.VISIBLE) {
            Log.d("MainActivity", "🔥 Hiding bottom navigation...")
            binding.mainMenuBottomNavigation.animate()
                .translationY(binding.mainMenuBottomNavigation.height.toFloat())
                .setDuration(300)
                .withEndAction {
                    binding.mainMenuBottomNavigation.visibility = View.GONE
                    Log.d("MainActivity", "🔥 Bottom navigation HIDDEN")
                }
                .start()
        }
    }

    // ✅ HÀM MỚI: Hiện menu với animation mượt
    private fun showBottomNav() {
        if (binding.mainMenuBottomNavigation.visibility != View.VISIBLE) {
            binding.mainMenuBottomNavigation.visibility = View.VISIBLE
            binding.mainMenuBottomNavigation.translationY = binding.mainMenuBottomNavigation.height.toFloat()
            binding.mainMenuBottomNavigation.animate()
                .translationY(0f)
                .setDuration(300)
                .withStartAction {
                    Log.d("MainActivity", "✅ Bottom navigation SHOWN")
                }
                .start()
        }
    }
    // ✅ THÊM: onResume để đảm bảo sync khi quay lại app
    override fun onResume() {
        super.onResume()
        // Đảm bảo ViewPager và Menu đồng bộ khi resume
        val currentItem = binding.mainBodyViewPager2.currentItem
        updateMenuSelection(currentItem)
    }
}