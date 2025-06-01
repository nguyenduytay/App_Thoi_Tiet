package com.example.weather2.Utils


import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import com.example.weather2.Model.Entity.data_humidity
import com.example.weather2.Model.Entity.data_humidity_land
import com.example.weather2.Model.Entity.data_pump
import com.example.weather2.Model.Entity.data_rain
import com.example.weather2.Model.Entity.data_temp
import com.example.weather2.View.DataActionDialog
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV Export Helper - Ổn định, nhẹ, không cần thư viện ngoài
 */
class CSVExportHelper {

    companion object {
        private const val TAG = "CSVExport"

        /**
         * Xuất dữ liệu ra file CSV - Luôn hoạt động ổn định
         */
        @SuppressLint("NewApi")
        fun exportDataToCSV(
            context: Context,
            dataType: DataActionDialog.DataType,
            data: List<Any>,
            startDate: String,
            endDate: String
        ): Pair<Boolean, String> {
            return try {
                android.util.Log.d(TAG, "🚀 Bắt đầu xuất CSV: ${data.size} records")

                // Tạo thư mục
                val exportDir = getExportDirectory()
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                // Tạo file
                val fileName = generateFileName(dataType, startDate, endDate)
                val file = File(exportDir, fileName)

                android.util.Log.d(TAG, "📄 File path: ${file.absolutePath}")

                // Ghi CSV
                FileWriter(file, Charsets.UTF_8).use { writer ->
                    // BOM cho UTF-8 (giúp Excel mở đúng tiếng Việt)
                    writer.write("\uFEFF")

                    writeCSVHeader(writer, dataType)
                    writeCSVData(writer, dataType, data)
                }

                android.util.Log.d(TAG, "✅ CSV exported successfully: ${file.length()} bytes")
                Pair(true, file.absolutePath)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error exporting CSV", e)
                Pair(false, "")
            }
        }

        /**
         * Ghi header CSV
         */
        private fun writeCSVHeader(writer: FileWriter, dataType: DataActionDialog.DataType) {
            val headers = when (dataType) {
                DataActionDialog.DataType.HUMIDITY_LAND ->
                    "STT,Thời gian,Độ ẩm đất (%),Trạng thái\n"
                DataActionDialog.DataType.HUMIDITY ->
                    "STT,Thời gian,Độ ẩm không khí (%),Trạng thái\n"
                DataActionDialog.DataType.TEMP ->
                    "STT,Thời gian,Nhiệt độ (°C),Trạng thái\n"
                DataActionDialog.DataType.RAIN ->
                    "STT,Thời gian,Giá trị cảm biến,Trạng thái mưa\n"
                DataActionDialog.DataType.PUMP ->
                    "STT,Thời gian,Trạng thái,Mô tả\n"
            }
            writer.write(headers)
        }

        /**
         * Ghi dữ liệu CSV
         */
        private fun writeCSVData(writer: FileWriter, dataType: DataActionDialog.DataType, data: List<Any>) {
            when (dataType) {
                DataActionDialog.DataType.HUMIDITY_LAND -> {
                    data.filterIsInstance<data_humidity_land>().forEachIndexed { index, item ->
                        writer.write("${index + 1},")
                        writer.write("\"${formatTimestamp(item.time)}\",")
                        writer.write("${item.humidity_land},")
                        writer.write("\"${getHumidityLandStatus(item.humidity_land)}\"\n")
                    }
                }

                // ✅ SỬA LỖI: HUMIDITY - Dùng sai function status
                DataActionDialog.DataType.HUMIDITY -> {
                    data.filterIsInstance<data_humidity>().forEachIndexed { index, item ->
                        writer.write("${index + 1},")
                        writer.write("\"${formatTimestamp(item.time)}\",")
                        writer.write("${item.humidity},")
                        writer.write("\"${getHumidityStatus(item.humidity)}\"\n")
                    }
                }

                // ✅ SỬA LỖI: TEMP - Dùng sai function status
                DataActionDialog.DataType.TEMP -> {
                    data.filterIsInstance<data_temp>().forEachIndexed { index, item ->
                        writer.write("${index + 1},")
                        writer.write("\"${formatTimestamp(item.time)}\",")
                        writer.write("${item.temp},")
                        writer.write("\"${getTempStatus(item.temp)}\"\n")
                    }
                }

                DataActionDialog.DataType.RAIN -> {
                    data.filterIsInstance<data_rain>().forEachIndexed { index, item ->
                        val status = if (item.rain >= 3000) "Không mưa" else "Có mưa"
                        writer.write("${index + 1},")
                        writer.write("\"${formatTimestamp(item.time)}\",")
                        writer.write("${item.rain},")
                        writer.write("\"$status\"\n")
                    }
                }

                DataActionDialog.DataType.PUMP -> {
                    data.filterIsInstance<data_pump>().forEachIndexed { index, item ->
                        val status = if (item.status) "BẬT" else "TẮT"
                        val description = if (item.status) "Máy bơm đang hoạt động" else "Máy bơm tắt"
                        writer.write("${index + 1},")
                        writer.write("\"${formatTimestamp(item.time)}\",")
                        writer.write("\"$status\",")
                        writer.write("\"$description\"\n")
                    }
                }
            }
        }

        /**
         * Tạo tên file
         */
        private fun generateFileName(dataType: DataActionDialog.DataType, startDate: String, endDate: String): String {
            val dataTypeName = when (dataType) {
                DataActionDialog.DataType.HUMIDITY_LAND -> "DoAmDat"
                DataActionDialog.DataType.TEMP -> "NhietDo"
                DataActionDialog.DataType.HUMIDITY -> "DoKhongKhi"
                DataActionDialog.DataType.RAIN -> "TrangThaiMua"
                DataActionDialog.DataType.PUMP -> "TrangThaiBom"
            }
            val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            return "${dataTypeName}_${startDate}_${endDate}_${currentTime}.csv"
        }

        /**
         * Lấy thư mục xuất file
         */
        private fun getExportDirectory(): File {
            return File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "IoT_Data_Export"
            )
        }

        /**
         * Format timestamp
         */
        private fun formatTimestamp(timestamp: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                timestamp
            }
        }

        /**
         * Lấy trạng thái độ ẩm đất, nhiệt độ, độ ẩm không khí
         */
        private fun getHumidityLandStatus(humidity: Int): String {
            return when {
                humidity < 30 -> "Thấp - Cần tưới"
                humidity > 80 -> "Cao - Nguy cơ úng"
                else -> "Bình thường"
            }
        }

        private fun getHumidityStatus(humidity: Double): String {
            return when {
                humidity < 40.0 -> "Thấp"
                humidity > 80.0 -> "Cao"
                else -> "Bình thường"
            }
        }

        private fun getTempStatus(temp: Double): String {
            return when {
                temp < 20.0 -> "Mát"
                temp > 35.0 -> "Nóng"
                else -> "Bình thường"
            }
        }
    }
}