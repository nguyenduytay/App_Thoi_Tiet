package com.example.weather2.Utils

import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class TimeAxisValueFormatter(private val timeData: List<String>) : ValueFormatter() {
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index >= 0 && index < timeData.size) {
            try {
                val date = inputFormat.parse(timeData[index])
                if (date != null) {
                    outputFormat.format(date)
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }
}