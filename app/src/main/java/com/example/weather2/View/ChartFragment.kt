package com.example.weather2.View

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather2.Adapter.DataTableAdapter
import com.example.weather2.Model.Entity.data_humidity
import com.example.weather2.Model.Entity.data_humidity_land
import com.example.weather2.Model.Entity.data_pump
import com.example.weather2.Model.Entity.data_rain
import com.example.weather2.Model.Entity.data_temp
import com.example.weather2.Model.ItemAdapter.itemDataTableAdapter
import com.example.weather2.R
import com.example.weather2.Utils.CSVExportHelper
import com.example.weather2.Utils.TimeAxisValueFormatter
import com.example.weather2.ViewModel.DataHumidityLandViewModel
import com.example.weather2.ViewModel.DataHumidityViewModel
import com.example.weather2.ViewModel.DataPumpViewModel
import com.example.weather2.ViewModel.DataRainViewModel
import com.example.weather2.ViewModel.DataTempViewModel
import com.example.weather2.databinding.FragmentChartBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChartFragment : Fragment() {

    // ==================== VÙNG 1: KHAI BÁO BIẾN & PROPERTIES ====================

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private lateinit var dataHumidityLandViewModel: DataHumidityLandViewModel
    private lateinit var dataPumpViewModel: DataPumpViewModel
    private lateinit var dataRainViewModel: DataRainViewModel
    private lateinit var dataTempViewModel: DataTempViewModel
    private lateinit var dataHumidityViewModel: DataHumidityViewModel

    // Filter states
    private var selectedDateRange = "today"           // today, yesterday, last_week, custom
    private var selectedDataType = "humidity_land"    // humidity_land, humidity, temp
    private var customStartDate = ""                  // Custom start date
    private var customEndDate = ""                    // Custom end date
    private var isAdvancedFiltersExpanded = false     // Advanced filters state

    // Adapters
    private lateinit var tableAdapter: DataTableAdapter

    // Flags
    private var isFirstLoad = true

    // ==================== VÙNG 2: LIFECYCLE METHODS ====================

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        initViewModels()
        setupUi()
        observeViewModels()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (!isFirstLoad) {
            animateChartsOnRefresh()
        }
        isFirstLoad = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== VÙNG 3: KHỞI TẠO & SETUP ====================

    /**
     * Khởi tạo tất cả ViewModels
     */
    private fun initViewModels() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)

        dataHumidityLandViewModel = ViewModelProvider(this, factory)[DataHumidityLandViewModel::class.java]
        dataPumpViewModel = ViewModelProvider(this, factory)[DataPumpViewModel::class.java]
        dataRainViewModel = ViewModelProvider(this, factory)[DataRainViewModel::class.java]
        dataTempViewModel = ViewModelProvider(this, factory)[DataTempViewModel::class.java]
        dataHumidityViewModel = ViewModelProvider(this, factory)[DataHumidityViewModel::class.java]
    }

    /**
     * Thiết lập giao diện cơ bản
     */
    private fun setupUi() {
        // Swipe refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        // Setup các components
        setupCharts()
        setupRecyclerView()
        setupDateRangeTabEvents()
        setupDataTypeButtonEvents()
        setupDisplayModeTabEvents()
        setupCustomDateEvents()
        setupButtonColors()
        setupManagementButtonEvents()

        // Set ngày mặc định
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.buttonStartDate.text = today
        binding.buttonEndDate.text = today
    }

    // ==================== VÙNG 4: SETUP CHARTS ====================

    /**
     * Thiết lập tất cả các biểu đồ
     */
    private fun setupCharts() {
        setupPumpChart()      // Biểu đồ máy bơm
        setupRainChart()      // Biểu đồ mưa
        setupHistoryChart()   // Biểu đồ lịch sử
    }

    /**
     * Thiết lập biểu đồ trạng thái máy bơm
     */
    private fun setupPumpChart() {
        binding.pumpChart.apply {
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            description.isEnabled = false
            setBackgroundColor(Color.WHITE)

            // Thiết lập trục X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)

                labelRotationAngle = -45f
                textSize = 8f
                textColor = Color.BLACK
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
                labelRotationAngle = 0f
            }

            // Thiết lập trục Y cho ON/OFF
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 1.2f
                textColor = Color.parseColor("#FF9800")  // Màu cam
                setLabelCount(3, false)
                textSize = 10f

                setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when {
                            value <= 0.1f -> "OFF"
                            value >= 0.9f -> "ON"
                            else -> ""
                        }
                    }
                })
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
        }
    }

    /**
     * Thiết lập biểu đồ cảm biến mưa
     */
    private fun setupRainChart() {
        binding.rainChart.apply {
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            description.isEnabled = false
            setBackgroundColor(Color.WHITE)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                labelRotationAngle = -45f
                textSize = 8f
                textColor = Color.BLACK
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 1f
                granularity = 1f
                setLabelCount(2, true)
                isGranularityEnabled = true
                textColor = Color.CYAN
                textSize = 10f

                setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when {
                            value <= 0.1f -> "Khô"
                            value >= 0.9f -> "Mưa"
                            else -> ""
                        }
                    }
                })
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
        }
    }

    /**
     * Thiết lập biểu đồ lịch sử dữ liệu
     */
    private fun setupHistoryChart() {
        binding.historyChart.apply {
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            description.isEnabled = false
            setBackgroundColor(Color.WHITE)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                granularity = 1f
                textSize = 10f
                textColor = Color.BLACK
                setLabelCount(8, false)
                labelRotationAngle = -45f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.BLUE
                textSize = 10f
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
        }
    }

    // ==================== VÙNG 5: SETUP UI COMPONENTS ====================

    /**
     * Thiết lập RecyclerView cho bảng dữ liệu
     */
    private fun setupRecyclerView() {
        tableAdapter = DataTableAdapter(emptyList())

        binding.rcDataTable.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tableAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    /**
     * Setup màu mặc định cho data type buttons
     */
    private fun setupButtonColors() {
        val normalColor = Color.parseColor("#928282")
        val selectedColor = Color.parseColor("#4CAF50")

        binding.btHumidityLandStatus.backgroundTintList = ColorStateList.valueOf(selectedColor)
        binding.btHumidityStatus.backgroundTintList = ColorStateList.valueOf(normalColor)
        binding.btTempStatus.backgroundTintList = ColorStateList.valueOf(normalColor)
    }

    /**
     * Cập nhật màu cho data type buttons
     */
    private fun updateButtonColors(selectedButton: Button) {
        val normalColor = Color.parseColor("#928282")
        val selectedColor = Color.parseColor("#4CAF50")

        val buttons = listOf(binding.btHumidityLandStatus, binding.btHumidityStatus, binding.btTempStatus)
        buttons.forEach { button ->
            if (button == selectedButton) {
                button.backgroundTintList = ColorStateList.valueOf(selectedColor)
                button.setTextColor(Color.BLACK)
                button.elevation = 8f
            } else {
                button.backgroundTintList = ColorStateList.valueOf(normalColor)
                button.setTextColor(Color.BLACK)
                button.elevation = 2f
            }
        }
    }

    // ==================== VÙNG 6: EVENT HANDLERS ====================

    /**
     * Sự kiện TabLayout chọn khoảng thời gian
     */
    private fun setupDateRangeTabEvents() {
        binding.tabLayoutDateRange.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        selectedDateRange = "today"
                        binding.layoutCustomDateRange.visibility = View.GONE
                    }
                    1 -> {
                        selectedDateRange = "yesterday"
                        binding.layoutCustomDateRange.visibility = View.GONE
                    }
                    2 -> {
                        selectedDateRange = "last_week"
                        binding.layoutCustomDateRange.visibility = View.GONE
                    }
                    3 -> {
                        selectedDateRange = "custom"
                        binding.layoutCustomDateRange.visibility = View.VISIBLE
                    }
                }
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    /**
     * Sự kiện Button chọn loại dữ liệu
     */
    private fun setupDataTypeButtonEvents() {
        binding.btHumidityLandStatus.setOnClickListener {
            selectedDataType = "humidity_land"
            updateButtonColors(binding.btHumidityLandStatus)
            binding.tvTitleChart.text = "Độ ẩm đất"
            applyFilters()
        }

        binding.btHumidityStatus.setOnClickListener {
            selectedDataType = "humidity"
            updateButtonColors(binding.btHumidityStatus)
            binding.tvTitleChart.text = "Độ ẩm không khí"
            applyFilters()
        }

        binding.btTempStatus.setOnClickListener {
            selectedDataType = "temp"
            updateButtonColors(binding.btTempStatus)
            binding.tvTitleChart.text = "Nhiệt độ"
            applyFilters()
        }
    }

    /**
     * Sự kiện TabLayout chuyển đổi chế độ hiển thị
     */
    private fun setupDisplayModeTabEvents() {
        binding.tabLayoutDisplayMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.viewFlipperDisplayMode.displayedChild = 0
                        animateChart()
                    }
                    1 -> {
                        binding.viewFlipperDisplayMode.displayedChild = 1
                        loadTableData()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    /**
     * Sự kiện Custom Date Picker
     */
    private fun setupCustomDateEvents() {
        binding.buttonStartDate.setOnClickListener {
            showDatePicker { date ->
                customStartDate = date
                binding.buttonStartDate.text = formatDateForDisplay(date)
                if (selectedDateRange == "custom") {
                    applyFilters()
                }
            }
        }

        binding.buttonEndDate.setOnClickListener {
            showDatePicker { date ->
                customEndDate = date
                binding.buttonEndDate.text = formatDateForDisplay(date)
                if (selectedDateRange == "custom") {
                    applyFilters()
                }
            }
        }
    }

    // ==================== VÙNG 7: DATA PROCESSING ====================

    /**
     * Áp dụng tất cả bộ lọc và cập nhật hiển thị
     */
    private fun applyFilters() {
        try {
            val filteredData = getFilteredData()

            when (binding.viewFlipperDisplayMode.displayedChild) {
                0 -> updateChart(filteredData)     // Chart mode
                1 -> updateTable(filteredData)     // Table mode
            }
        } catch (e: Exception) {
            android.util.Log.e("ChartFragment", "Error applying filters", e)
            Toast.makeText(context, "Lỗi áp dụng bộ lọc", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Lấy dữ liệu đã được lọc theo tất cả các tiêu chí
     */
    private fun getFilteredData(): List<Any> {
        val rawData = when (selectedDataType) {
            "humidity_land" -> dataHumidityLandViewModel.dataHumidityLandList.value ?: emptyList()
            "humidity" -> dataHumidityViewModel.dataHumidityList.value ?: emptyList()
            "temp" -> dataTempViewModel.dataTempList.value ?: emptyList()
            else -> emptyList()
        }

        val dateFilteredData = filterByDateRange(rawData)
        val advancedFilteredData = applyAdvancedFilters(dateFilteredData)

        return advancedFilteredData
    }

    /**
     * Lọc dữ liệu theo khoảng thời gian
     */
    private fun filterByDateRange(data: List<Any>): List<Any> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val (startDate, endDate) = when (selectedDateRange) {
            "today" -> {
                val today = dayFormat.format(Date())
                Pair(today, today)
            }
            "yesterday" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = dayFormat.format(calendar.time)
                Pair(yesterday, yesterday)
            }
            "last_week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                val startLastWeek = dayFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                val endLastWeek = dayFormat.format(calendar.time)
                Pair(startLastWeek, endLastWeek)
            }
            "custom" -> Pair(customStartDate, customEndDate)
            else -> {
                val today = dayFormat.format(Date())
                Pair(today, today)
            }
        }

        return data.filter { item ->
            val timeString = when (item) {
                is data_humidity_land -> item.time
                is data_humidity -> item.time
                is data_temp -> item.time
                else -> null
            }

            timeString?.let { time ->
                try {
                    val itemDate = dateFormat.parse(time)
                    val itemDay = dayFormat.format(itemDate ?: Date())
                    itemDay in startDate..endDate
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }
    }

    /**
     * Áp dụng advanced filters
     */
    private fun applyAdvancedFilters(data: List<Any>): List<Any> {
        return when (selectedDateRange) {
            "last_week", "custom" -> aggregateData(data)
            else -> data
        }
    }

    /**
     * Aggregate dữ liệu theo giờ/ngày
     */
    private fun aggregateData(data: List<Any>): List<Any> {
        // TODO: Implement aggregation logic if needed
        return data
    }

    /**
     * Sắp xếp dữ liệu theo thời gian
     */
    private fun sortDataByTime(data: List<Any>): List<Any> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return data.sortedBy { item ->
            val timeString = when (item) {
                is data_humidity_land -> item.time
                is data_humidity -> item.time
                is data_temp -> item.time
                else -> null
            }

            try {
                timeString?.let { dateFormat.parse(it)?.time } ?: 0L
            } catch (e: Exception) {
                android.util.Log.e("ChartFragment", "Error parsing time: $timeString", e)
                0L
            }
        }
    }

    // ==================== VÙNG 8: CHART UPDATES ====================

    /**
     * Cập nhật biểu đồ với dữ liệu đã lọc
     */
    private fun updateChart(data: List<Any>) {
        if (data.isEmpty()) {
            binding.historyChart.clear()
            binding.historyChart.invalidate()
            return
        }
        updateLineChart(data)
    }

    /**
     * Cập nhật biểu đồ đường
     */
    private fun updateLineChart(data: List<Any>) {
        try {
            val sortedData = sortDataByTime(data)

            val timeData = sortedData.map { item ->
                when (item) {
                    is data_humidity_land -> item.time
                    is data_humidity -> item.time
                    is data_temp -> item.time
                    else -> ""
                }
            }

            val entries = sortedData.mapIndexed { index, item ->
                val value = when (item) {
                    is data_humidity_land -> item.humidity_land.toFloat()
                    is data_humidity -> item.humidity.toFloat()
                    is data_temp -> item.temp.toFloat()
                    else -> 0f
                }
                Entry(index.toFloat(), value)
            }

            if (entries.isEmpty()) return

            val dataSet = LineDataSet(entries, getDataTypeLabel()).apply {
                color = getDataTypeColor()
                setCircleColor(getDataTypeColor())
                lineWidth = 2f
                circleRadius = 3f
                setDrawFilled(true)
                fillColor = getDataTypeColor()
                fillAlpha = 30
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
            }

            binding.historyChart.xAxis.valueFormatter = TimeAxisValueFormatter(timeData)

            val lineData = LineData(dataSet)
            binding.historyChart.data = lineData
            binding.historyChart.animateY(800)
            binding.historyChart.invalidate()

        } catch (e: Exception) {
            android.util.Log.e("ChartFragment", "Error updating line chart", e)
            Toast.makeText(context, "Lỗi cập nhật biểu đồ", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cập nhật biểu đồ máy bơm với timestamp thực
     */
    private fun updatePumpChart(pumpList: List<data_pump>) {
        if (pumpList.isEmpty()) return

        // ✅ THAY ĐỔI: Sử dụng Entry thay vì BarEntry
        val entries = mutableListOf<Entry>()

        pumpList.forEachIndexed { index, data ->
            // Chuyển đổi boolean thành float: true = 1.0 (ON), false = 0.0 (OFF)
            val value = if (data.status) 1.0f else 0.0f
            entries.add(Entry(index.toFloat(), value))  // Entry thay vì BarEntry
        }

        // ✅ THAY ĐỔI: Tạo LineDataSet thay vì BarDataSet (giống biểu đồ mưa)
        val dataSet = LineDataSet(entries, "Máy bơm (ON/OFF)").apply {
            color = Color.parseColor("#FF9800")     // Màu cam
            setCircleColor(Color.parseColor("#FF9800"))  // Màu điểm cam
            lineWidth = 3f                          // Đường dày (giống mưa)
            circleRadius = 4f                       // Điểm to (giống mưa)
            setDrawCircleHole(false)                // Không vẽ lỗ ở giữa điểm
            valueTextSize = 9f                      // Kích thước text
            setDrawFilled(true)                     // Tô màu vùng dưới đường
            fillColor = Color.parseColor("#FF9800") // Màu tô cam
            fillAlpha = 50                          // Độ trong suốt (giống mưa)
            mode = LineDataSet.Mode.STEPPED         // Đường bậc thang (giống mưa - phù hợp với dữ liệu binary)
            setDrawValues(false)                    // Không hiển thị giá trị trên điểm
        }

        // Set formatter cho trục X với thời gian thực tế
        val timeData = pumpList.map { it.time }
        binding.pumpChart.xAxis.apply {
            granularity = 1f
            valueFormatter = TimeAxisValueFormatter(timeData)
            labelRotationAngle = -45f
        }

        val lineData = LineData(dataSet)

        binding.pumpChart.apply {
            data = lineData  // Gán LineData
            if (!binding.swipeRefreshLayout.isRefreshing) {
                animateY(800)
            }
            invalidate()
        }
    }

    /**
     * Cập nhật biểu đồ mưa với timestamp thực
     */
    private fun updateRainChart(rainList: List<data_rain>) {
        if (rainList.isEmpty()) return

        val entries = mutableListOf<Entry>()

        rainList.forEachIndexed { index, data ->
            // Logic cảm biến mưa: >= 3000 = không mưa (0), < 3000 = có mưa (1)
            // Giá trị sensor cao = khô, giá trị thấp = ướt
            val rainValue = if (data.rain >= 3000) 0f else 1f
            entries.add(Entry(index.toFloat(), rainValue))
        }

        // Tạo LineDataSet với styling
        val dataSet = LineDataSet(entries, "Mưa (Có/Không)").apply {
            color = Color.CYAN                   // Màu cyan
            setCircleColor(Color.CYAN)          // Màu điểm
            lineWidth = 3f                      // Đường dày hơn
            circleRadius = 4f                   // Điểm to hơn
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)                 // Tô màu vùng dưới
            fillColor = Color.CYAN
            fillAlpha = 50                      // Đậm hơn một chút
            mode = LineDataSet.Mode.STEPPED     // Đường bậc thang (phù hợp với dữ liệu binary)
            setDrawValues(false)

        }

        // Set formatter cho trục X
        val timeData = rainList.map { it.time }
        binding.rainChart.xAxis.apply {
            granularity = 1f
            valueFormatter = TimeAxisValueFormatter(timeData)
            labelRotationAngle = -45f
        }

        // Cập nhật biểu đồ
        val lineData = LineData(dataSet)
        binding.rainChart.apply {
            data = lineData
            if (!binding.swipeRefreshLayout.isRefreshing) {
                animateY(1000)
            }
            invalidate()
        }
    }

    // ==================== VÙNG 9: TABLE UPDATES ====================

    /**
     * Cập nhật bảng với dữ liệu đã lọc
     */
    private fun updateTable(data: List<Any>) {
        val tableData = data.map { item ->
            when (item) {
                is data_humidity_land -> itemDataTableAdapter(
                    time = formatTimestamp(item.time),
                    value = "${item.humidity_land} %",
                    status = getHumidityLandStatus(item.humidity_land)
                )
                is data_humidity -> itemDataTableAdapter(
                    time = formatTimestamp(item.time),
                    value = "${item.humidity} %",
                    status = getHumidityStatus(item.humidity)
                )
                is data_temp -> itemDataTableAdapter(
                    time = formatTimestamp(item.time),
                    value = "${item.temp} °C",
                    status = getTempStatus(item.temp)
                )
                else -> itemDataTableAdapter("", "", "")
            }
        }
        tableAdapter.updateData(tableData)
    }

    // ==================== VÙNG 10: OBSERVERS ====================

    /**
     * Thiết lập việc lắng nghe dữ liệu từ ViewModels
     */
    private fun observeViewModels() {
        observeChartData()
        observeHistoryData()
    }

    /**
     * Lắng nghe dữ liệu cho các biểu đồ pump và rain
     */
    private fun observeChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataPumpViewModel.dataPumpList.collect { pumpList ->
                    updatePumpChart(getLatestPoints(pumpList))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataRainViewModel.dataRainList.collect { rainList ->
                    updateRainChart(getLatestPoints(rainList))
                }
            }
        }
    }

    /**
     * Lắng nghe dữ liệu cho history chart
     */
    private fun observeHistoryData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataHumidityLandViewModel.dataHumidityLandList.collect {
                    if (selectedDataType == "humidity_land") {
                        applyFilters()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataHumidityViewModel.dataHumidityList.collect {
                    if (selectedDataType == "humidity") {
                        applyFilters()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataTempViewModel.dataTempList.collect {
                    if (selectedDataType == "temp") {
                        applyFilters()
                    }
                }
            }
        }
    }

    // ==================== VÙNG 11: HELPER FUNCTIONS ====================

    /**
     * Lấy điểm dữ liệu gần nhất
     */
    private fun <T> getLatestPoints(dataList: List<T>): List<T> {
        if (dataList.isEmpty()) return emptyList()

        val sortedList = dataList.sortedBy { data ->
            when (data) {
                is data_pump -> {
                    try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        dateFormat.parse(data.time)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
                is data_rain -> {
                    try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        dateFormat.parse(data.time)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
                else -> 0L
            }
        }

        return sortedList.takeLast(9)
    }

    /**
     * Lấy trạng thái độ ẩm đất
     */
    private fun getHumidityLandStatus(humidity: Int): String {
        return when {
            humidity < 30 -> "Thấp"
            humidity > 80 -> "Cao"
            else -> "Bình thường"
        }
    }

    /**
     * Lấy trạng thái độ ẩm không khí
     */
    private fun getHumidityStatus(humidity: Double): String {
        return when {
            humidity < 40 -> "Thấp"
            humidity > 80 -> "Cao"
            else -> "Bình thường"
        }
    }

    /**
     * Lấy trạng thái nhiệt độ
     */
    private fun getTempStatus(temp: Double): String {
        return when {
            temp < 20 -> "Thấp"
            temp > 35 -> "Cao"
            else -> "Bình thường"
        }
    }

    /**
     * Lấy label cho data type hiện tại
     */
    private fun getDataTypeLabel(): String {
        return when (selectedDataType) {
            "humidity_land" -> "Độ ẩm đất (%)"
            "humidity" -> "Độ ẩm không khí (%)"
            "temp" -> "Nhiệt độ (°C)"
            else -> ""
        }
    }

    /**
     * Lấy màu cho data type hiện tại
     */
    private fun getDataTypeColor(): Int {
        return when (selectedDataType) {
            "humidity_land" -> Color.BLUE
            "humidity" -> Color.CYAN
            "temp" -> Color.parseColor("#FF9800")
            else -> Color.BLACK
        }
    }

    /**
     * Format timestamp từ yyyy-MM-dd HH:mm:ss thành dd/MM HH:mm
     */
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            timestamp
        }
    }

    /**
     * Format ngày từ yyyy-MM-dd thành dd/MM/yyyy
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
     * Hiển thị DatePicker cho custom date selection
     */
    @SuppressLint("DefaultLocale")
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
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

    // ==================== VÙNG 12: ANIMATIONS & REFRESH ====================

    /**
     * Animation cho chart khi chuyển tab
     */
    private fun animateChart() {
        binding.historyChart.animateXY(1000, 800)
    }

    /**
     * Load dữ liệu cho bảng
     */
    private fun loadTableData() {
        applyFilters()
    }

    /**
     * Refresh dữ liệu khi người dùng swipe xuống
     */
    private fun refreshData() {
        binding.swipeRefreshLayout.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                animateChartsOnRefresh()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * Chạy animation cho tất cả biểu đồ khi refresh
     */
    private fun animateChartsOnRefresh() {
        binding.pumpChart.postDelayed({
            binding.pumpChart.animateY(1000)
        }, 200)

        binding.rainChart.postDelayed({
            binding.rainChart.animateY(1000)
        }, 400)
        binding.historyChart.postDelayed({
            binding.historyChart.animateY(1000)
        }, 400)
    }

// ===================================
// QUẢN LÝ DỮ LIỆU - XÓA VÀ XUẤT
// ===================================

    /**
     * Thiết lập sự kiện cho các button quản lý dữ liệu
     */
    private fun setupManagementButtonEvents() {
        // Button Xóa dữ liệu
        binding.btDelete.setOnClickListener {
            requireContext().showDataActionDialog(
                actionType = DataActionDialog.ActionType.DELETE
            ) { dataType, startDate, endDate ->
                handleDeleteData(dataType, startDate, endDate)
            }
        }

        // Button Xuất Excel
        binding.btExportExcel.setOnClickListener {
            requireContext().showDataActionDialog(
                actionType = DataActionDialog.ActionType.EXPORT
            ) { dataType, startDate, endDate ->
                handleExportData(dataType, startDate, endDate)
            }
        }
    }
// ===================================
// XỬ LÝ XÓA DỮ LIỆU
// ===================================

    /**
     * Xử lý yêu cầu xóa dữ liệu - hiển thị dialog xác nhận
     */
    private fun handleDeleteData(dataType: DataActionDialog.DataType, startDate: String, endDate: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa dữ liệu")
            .setMessage("Bạn có chắc chắn muốn xóa dữ liệu ${getDataTypeName(dataType)} từ ${formatDateForDisplay(startDate)} đến ${formatDateForDisplay(endDate)}?\n\nDữ liệu sẽ bị xóa vĩnh viễn và không thể khôi phục!")
            .setPositiveButton("Xóa") { _, _ ->
                performDeleteData(dataType, startDate, endDate)
            }
            .setNegativeButton("Hủy", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    /**
     * Thực hiện xóa dữ liệu
     */
    private fun performDeleteData(dataType: DataActionDialog.DataType, startDate: String, endDate: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            var loadingDialog: androidx.appcompat.app.AlertDialog? = null

            try {
                // Hiển thị loading
                loadingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Đang xóa dữ liệu...")
                    .setMessage("Vui lòng đợi...")
                    .setCancelable(false)
                    .create()
                loadingDialog.show()

                // Thực hiện xóa dữ liệu
                when (dataType) {
                    DataActionDialog.DataType.HUMIDITY_LAND -> {
                        dataHumidityLandViewModel.deleteDataByDateRange(startDate, endDate)
                        android.util.Log.d("DELETE", "Xóa dữ liệu độ ẩm đất từ $startDate đến $endDate")
                    }

                    // ✅ SỬA LỖI: HUMIDITY - Dùng sai ViewModel
                    DataActionDialog.DataType.HUMIDITY -> {
                        dataHumidityViewModel.deleteDataByDateRange(startDate, endDate)  // ✅ Sửa: dùng dataHumidityViewModel
                        android.util.Log.d("DELETE", "Xóa dữ liệu độ ẩm không khí từ $startDate đến $endDate")
                    }

                    DataActionDialog.DataType.TEMP -> {
                        dataTempViewModel.deleteDataByDateRange(startDate, endDate)
                        android.util.Log.d("DELETE", "Xóa dữ liệu nhiệt độ từ $startDate đến $endDate")
                    }

                    DataActionDialog.DataType.RAIN -> {
                        dataRainViewModel.deleteDataByDateRange(startDate, endDate)
                        android.util.Log.d("DELETE", "Xóa dữ liệu mưa từ $startDate đến $endDate")
                    }

                    DataActionDialog.DataType.PUMP -> {
                        dataPumpViewModel.deleteDataByDateRange(startDate, endDate)
                        android.util.Log.d("DELETE", "Xóa dữ liệu bơm từ $startDate đến $endDate")
                    }
                }

                // Simulate delay (remove when implement real delete)
                kotlinx.coroutines.delay(1000)

                loadingDialog.dismiss()

                // Hiển thị kết quả thành công
                showSuccessDialog(
                    title = "Thành công",
                    message = "Đã xóa dữ liệu ${getDataTypeName(dataType)} thành công!"
                ) {
                    applyFilters() // Refresh dữ liệu
                }

            } catch (e: Exception) {
                loadingDialog?.dismiss()
                showErrorDialog("Có lỗi xảy ra khi xóa dữ liệu: ${e.message}")
                android.util.Log.e("DELETE_ERROR", "Lỗi xóa dữ liệu", e)
            }
        }
    }
// ===================================
// XỬ LÝ XUẤT DỮ LIỆU
// ===================================

    /**
     * Xử lý yêu cầu xuất dữ liệu ra CSV
     */
    private fun handleExportData(dataType: DataActionDialog.DataType, startDate: String, endDate: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            var loadingDialog: androidx.appcompat.app.AlertDialog? = null

            try {
                android.util.Log.d("CSV_EXPORT", "🚀 Bắt đầu xuất CSV: $dataType từ $startDate đến $endDate")

                // Hiển thị loading
                loadingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Đang xuất dữ liệu...")
                    .setMessage("Vui lòng đợi...")
                    .setCancelable(false)
                    .create()
                loadingDialog.show()

                // Lấy dữ liệu theo loại và khoảng thời gian
                val dataToExport = getDataForExport(dataType, startDate, endDate)

                loadingDialog.dismiss()

                // Kiểm tra dữ liệu có tồn tại không
                if (dataToExport.isEmpty()) {
                    android.util.Log.w("CSV_EXPORT", "⚠️ Không có dữ liệu để xuất")
                    showInfoDialog(
                        title = "Thông báo",
                        message = "Không có dữ liệu ${getDataTypeName(dataType)} trong khoảng thời gian đã chọn!"
                    )
                    return@launch
                }

                android.util.Log.d("CSV_EXPORT", "📊 Tìm thấy ${dataToExport.size} bản ghi để xuất")

                // Thực hiện xuất CSV
                val result = CSVExportHelper.exportDataToCSV(
                    context = requireContext(),
                    dataType = dataType,
                    data = dataToExport,
                    startDate = startDate,
                    endDate = endDate
                )

                if (result.first) {
                    android.util.Log.d("CSV_EXPORT", "✅ Xuất CSV thành công: ${result.second}")
                    showExportSuccessDialog(result.second)
                } else {
                    android.util.Log.e("CSV_EXPORT", "❌ Xuất CSV thất bại")
                    showErrorDialog("Không thể xuất dữ liệu CSV. Vui lòng thử lại.")
                }

            } catch (e: Exception) {
                loadingDialog?.dismiss()
                android.util.Log.e("CSV_EXPORT", "💥 Lỗi xuất CSV", e)
                showErrorDialog("Có lỗi xảy ra khi xuất dữ liệu: ${e.message}")
            }
        }
    }
    /**
     * Lấy dữ liệu để xuất CSV
     */
    private fun getDataForExport(dataType: DataActionDialog.DataType, startDate: String, endDate: String): List<Any> {
        return when (dataType) {
            DataActionDialog.DataType.HUMIDITY_LAND -> {
                val allData = dataHumidityLandViewModel.dataHumidityLandList.value ?: emptyList()
                filterDataByDateRange(allData, startDate, endDate)
            }

            DataActionDialog.DataType.HUMIDITY -> {
                val allData = dataHumidityViewModel.dataHumidityList.value ?: emptyList()
                filterDataByDateRange(allData, startDate, endDate)
            }

            DataActionDialog.DataType.TEMP -> {
                val allData = dataTempViewModel.dataTempList.value ?: emptyList()
                filterDataByDateRange(allData, startDate, endDate)
            }

            DataActionDialog.DataType.RAIN -> {
                val allData = dataRainViewModel.dataRainList.value ?: emptyList()
                filterDataByDateRange(allData, startDate, endDate)
            }

            DataActionDialog.DataType.PUMP -> {
                val allData = dataPumpViewModel.dataPumpList.value ?: emptyList()
                filterDataByDateRange(allData, startDate, endDate)
            }
        }
    }

    /**
     * Hiển thị dialog thành công với tùy chọn mở file
     */
    private fun showExportSuccessDialog(filePath: String) {
        val file = java.io.File(filePath)
        val fileSize = file.length() / 1024 // KB

        val message = """
        ✅ Đã xuất CSV thành công!
        
        📁 Vị trí: Downloads/IoT_Data_Export/
        📊 Kích thước: ${fileSize}KB
        📝 Tên file: ${file.name}
        
        💡 File CSV có thể mở bằng:
        • Microsoft Excel
        • Google Sheets  
        • LibreOffice Calc
        • Notepad++
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xuất dữ liệu thành công")
            .setMessage(message)
            .setPositiveButton("MỞ FILE") { _, _ ->
                openCSVFile(filePath)
            }
            .setNeutralButton("MỞ THU MỤC") { _, _ ->
                openExportFolder()
            }
            .setNegativeButton("ĐÓNG", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    /**
     * Mở file CSV
     */
    private fun openCSVFile(filePath: String) {
        try {
            android.util.Log.d("CSV_EXPORT", "📱 Attempting to open CSV: $filePath")

            val file = java.io.File(filePath)
            if (!file.exists()) {
                android.util.Log.e("CSV_EXPORT", "❌ File không tồn tại: $filePath")
                showErrorDialog("File không tồn tại: $filePath")
                return
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                startActivity(intent)
                android.util.Log.d("CSV_EXPORT", "✅ Đã mở CSV thành công")
            } catch (e: android.content.ActivityNotFoundException) {
                android.util.Log.w("CSV_EXPORT", "⚠️ Không tìm thấy ứng dụng mở CSV")
                showInstallCSVAppDialog()
            }

        } catch (e: Exception) {
            android.util.Log.e("CSV_EXPORT", "💥 Lỗi mở CSV", e)
            showErrorDialog("Không thể mở file CSV: ${e.message}")
        }
    }

    /**
     * Mở thư mục Downloads
     */
    private fun openExportFolder() {
        try {
            // Thử mở thư mục IoT_Data_Export
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(
                    android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FIoT_Data_Export"),
                    "resource/folder"
                )
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback: mở Downloads chung
                val downloadIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"),
                        "resource/folder"
                    )
                }
                startActivity(downloadIntent)
            }
        } catch (e: Exception) {
            showInfoDialog("Thông báo", "Vui lòng vào File Manager > Downloads > IoT_Data_Export để xem file")
        }
    }

    /**
     * Dialog gợi ý cài ứng dụng CSV
     */
    private fun showInstallCSVAppDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cần ứng dụng để mở CSV")
            .setMessage("Không tìm thấy ứng dụng để mở file CSV. Bạn có muốn tải Google Sheets hoặc Microsoft Excel không?")
            .setPositiveButton("Tải Sheets") { _, _ ->
                openPlayStore("com.google.android.apps.docs.editors.sheets")
            }
            .setNeutralButton("Tải Excel") { _, _ ->
                openPlayStore("com.microsoft.office.excel")
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /**
     * Mở Play Store để tải ứng dụng
     */
    private fun openPlayStore(packageName: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("market://details?id=$packageName")
            }
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            }
            startActivity(intent)
        }
    }

// ===================================
// HELPER FUNCTIONS CHO QUẢN LÝ DỮ LIỆU
// ===================================

    /**
     * Filter dữ liệu theo khoảng thời gian cho việc xuất/xóa
     */
    private fun filterDataByDateRange(data: List<Any>, startDate: String, endDate: String): List<Any> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return data.filter { item ->
            val timeString = when (item) {
                is data_humidity_land -> item.time
                is data_humidity -> item.time
                is data_temp -> item.time
                is data_rain -> item.time
                is data_pump -> item.time
                else -> null
            }

            timeString?.let { time ->
                try {
                    val itemDate = dateFormat.parse(time)
                    val itemDay = dayFormat.format(itemDate ?: Date())
                    itemDay in startDate..endDate
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }
    }

    /**
     * Lấy tên hiển thị của loại dữ liệu
     */
    private fun getDataTypeName(dataType: DataActionDialog.DataType): String {
        return when (dataType) {
            DataActionDialog.DataType.HUMIDITY_LAND -> "Độ ẩm đất"
            DataActionDialog.DataType.HUMIDITY -> "Độ ẩm không khí"
            DataActionDialog.DataType.TEMP -> "Nhiệt độ"
            DataActionDialog.DataType.RAIN -> "Trạng thái mưa"
            DataActionDialog.DataType.PUMP -> "Trạng thái máy bơm"
        }
    }
// ===================================
// DIALOG HELPER FUNCTIONS
// ===================================

    /**
     * Hiển thị dialog thành công
     */
    private fun showSuccessDialog(title: String, message: String, onOk: (() -> Unit)? = null) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                onOk?.invoke()
            }
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    /**
     * Hiển thị dialog lỗi
     */
    private fun showErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Lỗi")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    /**
     * Hiển thị dialog thông tin
     */
    private fun showInfoDialog(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Extension function để sử dụng dialog dễ dàng hơn
     */
    private fun Context.showDataActionDialog(
        actionType: DataActionDialog.ActionType,
        onExecute: (DataActionDialog.DataType, String, String) -> Unit
    ) {
        DataActionDialog(this, actionType, onExecute).show()
    }
}