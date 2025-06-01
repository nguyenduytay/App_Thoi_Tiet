package com.example.weather2.Ui.Line

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class WeatherItemLineTempeView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var value: Int = 0
    private var valueAfter: Int=0
    private var valueBefore: Int=0
    private var maxValue: Int = 0
    private var minValue: Int = 0

    private val paintLine = Paint().apply {
        color = Color.WHITE
        strokeWidth = 5f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val paintCircle = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setData(value: Int,valueAfter:Int, valueBefore:Int ,maxValue: Int,minValue:Int) {
        this.value = value
        this.valueAfter=valueAfter
        this.maxValue = maxValue
        this.minValue=minValue
        this.valueBefore=valueBefore
        invalidate() // Yêu cầu vẽ lại
    }

    override fun onDraw(canvas: Canvas) {

        canvas.translate(0f, 10f)
        val width = width
        val height = height-20
        //tính khoảng cách cẩn chia
        val division= Math.abs(maxValue-minValue)
        // Chia chiều cao thành các bậc
        val stepHeight = height / division

        // vẽ 2 đường về 2 phía
        //tính tọa độ y đặt điểm tâm nằm trong khoảng
        val y = (Math.abs(maxValue - value) * stepHeight).coerceIn(0, height)
        // Tính tọa độ trung tâm của điểm tròn
        val centerX = width / 2f
        val centerY = y.toFloat()


        //vẽ về bên trái
        if(valueBefore > 0)
        {
            //tính tọa độ điểm y bên trái
            val yBefore=((Math.abs(maxValue-valueBefore)*stepHeight+y)/2).coerceIn(0,height)

            // Vẽ đường thẳng ngang
            canvas.drawLine(0f, yBefore.toFloat(), centerX, centerY, paintLine)
        }
        //vẽ về bên phải
        if(valueAfter > 0)
        {
            //tính tọa độ y bên phải
            val yAfter= abs((Math.abs(maxValue-valueAfter)*stepHeight+y)/2).coerceIn(0,height)
            // Vẽ đường thẳng ngang
            canvas.drawLine(centerX, centerY, width.toFloat(), yAfter.toFloat(), paintLine)
        }

        // Vẽ chấm tròn
        canvas.drawCircle(centerX, centerY, 10f, paintCircle)

        super.onDraw(canvas)
    }
}