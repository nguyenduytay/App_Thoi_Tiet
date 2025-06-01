package com.example.weather2.View

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import com.example.weather2.databinding.LayoutDataActionDialogBinding
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog để chọn loại dữ liệu và khoảng thời gian cho việc xóa/xuất dữ liệu
 * ✅ CẬP NHẬT: Sử dụng TabLayout thay vì Button
 */
class DataActionDialog(
    private val context: Context,
    private val actionType: ActionType, // DELETE hoặc EXPORT
    private val onActionExecute: (DataType, String, String) -> Unit
) {

    enum class ActionType { DELETE, EXPORT }
    enum class DataType {
        HUMIDITY_LAND,    // Tab 0
        HUMIDITY,         // Tab 1
        TEMP,             // Tab 2
        PUMP,             // Tab 3
        RAIN              // Tab 4
    }

    private lateinit var dialog: Dialog
    private lateinit var binding: LayoutDataActionDialogBinding
    private var selectedDataType = DataType.HUMIDITY_LAND  // Mặc định tab đầu
    private var startDate = ""
    private var endDate = ""

    /**
     * Tạo và hiển thị dialog
     */
    fun show() {
        dialog = Dialog(context)
        binding = LayoutDataActionDialogBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        setupDialog()
        setupEventListeners()
        setDefaultValues()

        dialog.show()
    }

    /**
     * Thiết lập dialog properties
     */
    @SuppressLint("SetTextI18n")
    private fun setupDialog() {
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Thiết lập title và warning dựa trên action type
        when (actionType) {
            ActionType.DELETE -> {
                binding.tvDialogTitle.text = "Xóa dữ liệu"
                binding.btnExecute.text = "Xóa dữ liệu"
                binding.btnExecute.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                binding.layoutDeleteWarning.visibility = View.VISIBLE
            }
            ActionType.EXPORT -> {
                binding.tvDialogTitle.text = "Xuất dữ liệu"
                binding.btnExecute.text = "Xuất Excel"
                binding.btnExecute.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                binding.layoutDeleteWarning.visibility = View.GONE
            }
        }
    }

    /**
     * ✅ THAY ĐỔI: Thiết lập sự kiện với TabLayout
     */
    private fun setupEventListeners() {
        // ✅ SỰ KIỆN TABLAYOUT: Chọn loại dữ liệu qua tab
        setupDataTypeTabEvents()

        // Sự kiện chọn ngày (giữ nguyên)
        binding.btnStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                binding.btnStartDate.text = formatDateForDisplay(date)
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                binding.btnEndDate.text = formatDateForDisplay(date)
            }
        }

        // Sự kiện button điều khiển (giữ nguyên)
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnExecute.setOnClickListener {
            if (validateInput()) {
                onActionExecute(selectedDataType, startDate, endDate)
                dialog.dismiss()
            } else {
                showValidationError()
            }
        }
    }

    /**
     * ✅ THÊM MỚI: Thiết lập sự kiện cho TabLayout
     */
    private fun setupDataTypeTabEvents() {
        binding.tabLayoutDisplayMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedDataType = when (tab.position) {
                    0 -> DataType.HUMIDITY_LAND    // Độ ẩm đất
                    1 -> DataType.HUMIDITY         // Độ ẩm không khí
                    2 -> DataType.TEMP             // Nhiệt độ
                    3 -> DataType.PUMP             // Trạng thái bơm
                    4 -> DataType.RAIN             // Trạng thái mưa
                    else -> DataType.HUMIDITY_LAND
                }

                // Debug log để kiểm tra
                android.util.Log.d("DataActionDialog", "Selected data type: $selectedDataType (Tab ${tab.position})")
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Không cần xử lý
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Không cần xử lý
            }
        })
    }

    /**
     * ✅ CẬP NHẬT: Thiết lập giá trị mặc định
     */
    private fun setDefaultValues() {
        // Set ngày mặc định
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // Ngày bắt đầu: 30 ngày trước
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val thirtyDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        startDate = thirtyDaysAgo
        endDate = today

        binding.btnStartDate.text = formatDateForDisplay(startDate)
        binding.btnEndDate.text = formatDateForDisplay(endDate)

        // ✅ THAY ĐỔI: Set tab mặc định (tab đầu tiên)
        selectedDataType = DataType.HUMIDITY_LAND

        // ✅ TÙY CHỌN: Đặt tab được chọn (nếu muốn khác tab 0)
        binding.tabLayoutDisplayMode.getTabAt(0)?.select()  // Chọn tab đầu tiên
    }

    /**
     * Hiển thị DatePicker (giữ nguyên)
     */
    @SuppressLint("DefaultLocale")
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    /**
     * Format ngày để hiển thị (giữ nguyên)
     */
    private fun formatDateForDisplay(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate ?: Date())
        } catch (e: Exception) {
            date
        }
    }

    /**
     * Kiểm tra tính hợp lệ của input (giữ nguyên)
     */
    private fun validateInput(): Boolean {
        if (startDate.isEmpty() || endDate.isEmpty()) {
            return false
        }

        // Kiểm tra ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)

            if (start != null && end != null && start.after(end)) {
                return false
            }
        } catch (e: Exception) {
            return false
        }

        return true
    }

    /**
     * Hiển thị lỗi validation (giữ nguyên)
     */
    private fun showValidationError() {
        val message = when {
            startDate.isEmpty() || endDate.isEmpty() -> "Vui lòng chọn ngày bắt đầu và ngày kết thúc"
            !isValidDateRange() -> "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc"
            else -> "Dữ liệu không hợp lệ"
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Lỗi")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    /**
     * Kiểm tra khoảng ngày có hợp lệ không (giữ nguyên)
     */
    private fun isValidDateRange(): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)
            start != null && end != null && !start.after(end)
        } catch (e: Exception) {
            false
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    /**
     * ✅ THÊM: Function để set tab theo DataType (tùy chọn)
     */
    fun setSelectedDataType(dataType: DataType) {
        selectedDataType = dataType
        val tabPosition = when (dataType) {
            DataType.HUMIDITY_LAND -> 0
            DataType.HUMIDITY -> 1
            DataType.TEMP -> 2
            DataType.PUMP -> 3
            DataType.RAIN -> 4
        }
        binding.tabLayoutDisplayMode.getTabAt(tabPosition)?.select()
    }

    /**
     * ✅ THÊM: Function để get DataType name (cho debug hoặc display)
     */
    private fun getDataTypeName(dataType: DataType): String {
        return when (dataType) {
            DataType.HUMIDITY_LAND -> "Độ ẩm đất"
            DataType.HUMIDITY -> "Độ ẩm không khí"
            DataType.TEMP -> "Nhiệt độ"
            DataType.PUMP -> "Trạng thái bơm"
            DataType.RAIN -> "Trạng thái mưa"
        }
    }
}