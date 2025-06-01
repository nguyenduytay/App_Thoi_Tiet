package com.example.weather2.Ui.Time

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ClockView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#565D95")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var secondAngle = 0f
    private var minuteAngle = 0f
    private var hourAngle = 0f

    init {
        startClock()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) - 20

        // Vẽ mặt đồng hồ
        canvas.drawCircle(centerX, centerY, radius, paintCircle)

        // Vẽ các vạch trên đồng hồ
        drawClockMarks(canvas, centerX, centerY, radius)

        // Vẽ kim giờ
        drawHand(canvas, centerX, centerY, radius * 0.2f, hourAngle, Color.BLACK, 8f)

        // Vẽ kim phút
        drawHand(canvas, centerX, centerY, radius * 0.4f, minuteAngle, Color.BLACK, 6f)

        // Vẽ kim giây
        drawHand(canvas, centerX, centerY, radius * 0.6f, secondAngle, Color.RED, 3f)
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, length: Float, angle: Float, color: Int, strokeWidth: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        val adjustedAngle = angle - 90  // Dịch chuyển góc để 0° nằm ở vị trí 12h
        val endX = cx + length * cos(Math.toRadians(adjustedAngle.toDouble())).toFloat()
        val endY = cy + length * sin(Math.toRadians(adjustedAngle.toDouble())).toFloat()
        canvas.drawLine(cx, cy, endX, endY, paint)
    }

    private fun drawClockMarks(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3DC08")
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFC")
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble()) // Điều chỉnh để 12h hướng lên trên

            // Xác định vạch dài hay ngắn
            val startOffset = if (i % 3 == 0) 15f else 7f  // 12h, 3h, 6h, 9h dài hơn
            val startX = cx + (radius - startOffset) * cos(angle).toFloat()
            val startY = cy + (radius - startOffset) * sin(angle).toFloat()
            val endX = cx + radius * cos(angle).toFloat()
            val endY = cy + radius * sin(angle).toFloat()

            if (i % 3 == 0)
                canvas.drawLine(startX, startY, endX, endY, paint1)
            else
                canvas.drawLine(startX, startY, endX, endY, paint2)
        }

        // Vẽ số 12, 3, 6, 9
        val numberOffset = radius - 35  // Điều chỉnh khoảng cách số so với viền đồng hồ
        val numbers = mapOf(
            12 to Pair(cx, cy - numberOffset),
            3 to Pair(cx + numberOffset, cy + 8),  // Điều chỉnh vị trí một chút cho cân đối
            6 to Pair(cx, cy + numberOffset + 15),
            9 to Pair(cx - numberOffset, cy + 8)
        )

        for ((num, pos) in numbers) {
            canvas.drawText(num.toString(), pos.first, pos.second, textPaint)
        }
    }


    private fun startClock() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val calendar = Calendar.getInstance()
                val seconds = calendar.get(Calendar.SECOND)
                val minutes = calendar.get(Calendar.MINUTE)
                val hours = calendar.get(Calendar.HOUR_OF_DAY) % 12 // Định dạng 12 giờ

                secondAngle = seconds * 6f
                minuteAngle = minutes * 6f + seconds * 0.1f
                hourAngle = hours * 30f + minutes * 0.5f

                invalidate() // Vẽ lại đồng hồ
                handler.postDelayed(this, 1000) // Cập nhật mỗi giây
            }
        }
        handler.post(runnable)
    }
}
