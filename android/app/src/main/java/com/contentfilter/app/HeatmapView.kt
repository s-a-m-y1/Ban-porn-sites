package com.contentfilter.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * GitHub-style activity heatmap of blocked attempts.
 * 15 weeks x 7 days, 5 intensity levels.
 */
class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    data class Cell(val day: String, val count: Int, val week: Int, val dow: Int)

    private var cells: List<Cell> = emptyList()
    private var max = 1
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // colors: 5 levels (dark theme aware via isDark)
    private val lightColors = intArrayOf(
        0xFFE8E8E8.toInt(), 0xFF9BE09B.toInt(), 0xFF4CAF50.toInt(),
        0xFF2E7D32.toInt(), 0xFF1B5E20.toInt(),
    )
    private val darkColors = intArrayOf(
        0xFF1E2921.toInt(), 0xFF1E5B26.toInt(), 0xFF2E7D32.toInt(),
        0xFF4CAF50.toInt(), 0xFF8BE39C.toInt(),
    )
    private var dark = false
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        color = Color.GRAY
    }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFF1565C0.toInt()
    }

    var selectedDay: String? = null
        private set

    fun setData(dayToCount: Map<String, Int>, dark: Boolean) {
        this.dark = dark
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        // end of heatmap = today; go back 14 full weeks + current week (15 columns)
        val cells = mutableListOf<Cell>()
        val todayCal = cal.clone() as Calendar
        val dayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK) // SUN=1
        val offsetToEnd = 7 - dayOfWeek
        for (i in 0 until 15 * 7) {
            val c = (todayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offsetToEnd - i)
            }
            val key = dayFmt.format(c.time)
            val count = dayToCount[key] ?: 0
            val week = 14 - i / 7
            val dow = c.get(Calendar.DAY_OF_WEEK) - 1
            cells.add(Cell(key, count, week, dow))
        }
        this.cells = cells
        max = (dayToCount.values.maxOrNull() ?: 0).coerceAtLeast(1)
        selectedDay = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cells.isEmpty()) return
        val colors = if (dark) darkColors else lightColors
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = 4f
        val topPad = 22f
        val cellSize = ((w - gap * 15) / 15).coerceAtMost((h - topPad - gap * 7) / 7)
        val gridW = cellSize * 15 + gap * 15
        val startX = (w - gridW) / 2

        // month labels above
        var lastMonth = -1
        labelPaint.textSize = cellSize * 0.42f
        var lastLabelX = -99f
        cells.filter { it.dow == 0 }.forEach { cell ->
            val month = cell.day.substring(5, 7).toInt()
            if (month != lastMonth && month != 0) {
                val x = startX + cell.week * (cellSize + gap)
                if (x - lastLabelX > cellSize * 3) {
                    canvas.drawText(monthLabel(cell.day), x, topPad * 0.75f, labelPaint)
                    lastLabelX = x
                }
                lastMonth = month
            }
        }

        // cells
        for (cell in cells) {
            val level = when {
                cell.count <= 0 -> 0
                max <= 4 -> (cell.count * 4 / max).coerceAtLeast(1)
                else -> (cell.count * 4 / max).coerceAtLeast(1).coerceAtMost(4)
            }
            paint.color = colors[level]
            val x = startX + cell.week * (cellSize + gap)
            val y = topPad + cell.dow * (cellSize + gap)
            val r = cellSize * 0.22f
            canvas.drawRoundRect(RectF(x, y, x + cellSize, y + cellSize), r, r, paint)
        }

        // selected day outline
        selectedDay?.let { sel ->
            val cell = cells.firstOrNull { it.day == sel } ?: return
            val x = startX + cell.week * (cellSize + gap)
            val y = topPad + cell.dow * (cellSize + gap)
            canvas.drawRoundRect(
                RectF(x - 2, y - 2, x + cellSize + 2, y + cellSize + 2),
                cellSize * 0.22f, cellSize * 0.22f, selPaint,
            )
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN && cells.isNotEmpty()) {
            val w = width.toFloat()
            val h = height.toFloat()
            val gap = 4f
            val topPad = 22f
            val cellSize = ((w - gap * 15) / 15).coerceAtMost((h - topPad - gap * 7) / 7)
            val gridW = cellSize * 15 + gap * 15
            val startX = (w - gridW) / 2
            val week = ((event.x - startX) / (cellSize + gap)).toInt()
            val dow = ((event.y - topPad) / (cellSize + gap)).toInt()
            val cell = cells.firstOrNull { it.week == week && it.dow == dow }
            if (cell != null) {
                selectedDay = cell.day
                invalidate()
                listener?.invoke(cell.day, cell.count)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private var listener: ((String, Int) -> Unit)? = null
    fun onCellClick(l: (String, Int) -> Unit) {
        listener = l
    }

    private fun monthLabel(day: String): String {
        val m = day.substring(5, 7).toInt()
        val names = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return names[(m - 1).coerceIn(0, 11)]
    }
}
