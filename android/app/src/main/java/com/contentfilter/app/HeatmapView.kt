package com.contentfilter.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 15-week x 7-day blocking-activity grid, GitHub-style, drawn on canvas.
 *
 * Public contract (used by StatsFragment — keep stable):
 *   setData(dayToCount, dark) / onCellClick { day, count -> } / selectedDay
 *
 * RTL: the app is Arabic-first with supportsRtl, so the grid mirrors — the
 * oldest week sits at the reading-start edge and time flows toward the
 * reading-end edge. Cell drawing, the selection/today ring, and the touch
 * hit-test all flow through one [Grid] geometry helper so they can never
 * drift apart.
 */
class HeatmapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    data class Cell(val day: String, val count: Int, val week: Int, val dow: Int)

    var selectedDay: String? = null
        private set

    private var cells: List<Cell> = emptyList()
    private var max = 1
    private var dark = false
    private var todayKey = ""
    private var listener: ((String, Int) -> Unit)? = null

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Light ramp: empty = 12% teal wash (green_12) — a designed quiet grid,
    // not gray mush. Activity climbs teal -> brass -> clay: the hottest
    // days read warm, calm ones stay brand teal.
    private val lightRamp = intArrayOf(
        0x1E2F7A6B.toInt(), // 0 empty
        0xFF8FC4B6.toInt(), // 1 faint teal
        0xFF2F7A6B.toInt(), // 2 brand teal
        0xFFB8863B.toInt(), // 3 brass
        0xFFB4573E.toInt()  // 4 clay (peak)
    )

    // Dark ramp: empty = one step above the card surface; dim teal -> night
    // teal -> sand amber -> clay, matching the values-night brand tones.
    private val darkRamp = intArrayOf(
        0xFF2E4256.toInt(), // 0 empty (raised over surface #22344A)
        0xFF3E5E5B.toInt(), // 1 dim teal
        0xFF8FC4B6.toInt(), // 2 teal (night)
        0xFFC9A45C.toInt(), // 3 sand amber
        0xFFD08A6E.toInt()  // 4 clay (night)
    )

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.cairo_medium)
    }
    private val dowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.cairo_regular)
    }

    // Arabic-first labels. New copy lives inline here because res/values*
    // is outside this task's file ownership.
    private val monthNames = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    // dow: 0 = Sunday .. 6 = Saturday (Calendar.DAY_OF_WEEK - 1)
    private val dowNames = arrayOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

    // Sparse rows only (Mon / Wed / Fri), GitHub-style, to avoid clutter.
    private val labeledDows = intArrayOf(1, 3, 5)

    fun setData(dayToCount: Map<String, Int>, dark: Boolean) {
        this.dark = dark
        todayKey = dayFmt.format(System.currentTimeMillis())
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayCal = cal.clone() as Calendar
        val dayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK) // SUN=1
        val offsetToEnd = 7 - dayOfWeek
        val built = mutableListOf<Cell>()
        for (i in 0 until 15 * 7) {
            val c = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offsetToEnd - i) }
            val key = dayFmt.format(c.time)
            built.add(Cell(key, dayToCount[key] ?: 0, 14 - i / 7, c.get(Calendar.DAY_OF_WEEK) - 1))
        }
        cells = built
        max = (dayToCount.values.maxOrNull() ?: 0).coerceAtLeast(1)
        selectedDay = null
        invalidate()
    }

    fun onCellClick(l: (String, Int) -> Unit) { listener = l }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cells.isEmpty() || width <= 0 || height <= 0) return
        val g = grid(width, height)

        drawMonthHeader(canvas, g)
        drawWeekdayLabels(canvas, g)

        // Cells: rounded, evenly gapped, colored by intensity level.
        val ramp = if (dark) darkRamp else lightRamp
        val radius = g.cell * 0.30f
        val rect = RectF()
        for (cell in cells) {
            val x = g.colX(cell.week)
            val y = g.rowY(cell.dow)
            rect.set(x, y, x + g.cell, y + g.cell)
            cellPaint.color = ramp[levelOf(cell.count)]
            canvas.drawRoundRect(rect, radius, radius, cellPaint)
        }

        // Ring around the selected day — or today, so the default
        // "Today" readout and the grid agree before the first tap.
        val ringDay = selectedDay ?: todayKey
        cells.firstOrNull { it.day == ringDay }?.let { sel ->
            val x = g.colX(sel.week)
            val y = g.rowY(sel.dow)
            ringPaint.color = if (dark) 0xFF8FC4B6.toInt() else 0xFF2F7A6B.toInt()
            ringPaint.strokeWidth = (g.cell * 0.12f).coerceIn(3f, 6f)
            canvas.drawRoundRect(
                RectF(x - 2f, y - 2f, x + g.cell + 2f, y + g.cell + 2f),
                radius + 2f, radius + 2f, ringPaint
            )
        }
    }

    /** Month names along the top, one per month change, oldest -> newest
     *  (reading order in both directions — this also fixes the old loop,
     *  which walked newest-first with an LTR-only spacing check and so
     *  rendered at most one label). */
    private fun drawMonthHeader(canvas: Canvas, g: Grid) {
        monthPaint.textSize = g.monthSize
        monthPaint.color = if (dark) 0xFFC4BBAD.toInt() else 0xFF6F6759.toInt()
        monthPaint.textAlign = if (g.rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val y = g.monthBaseline
        var lastMonth = -1
        var lastAnchor = Float.MAX_VALUE
        // Each column holds exactly one Sunday; sorted by day = oldest first.
        for (sun in cells.filter { it.dow == 0 }.sortedBy { it.day }) {
            val m = sun.day.substring(5, 7).toInt() - 1
            if (m == lastMonth) continue
            lastMonth = m
            val anchor = if (g.rtl) g.colX(sun.week) + g.cell else g.colX(sun.week)
            val distance = if (g.rtl) lastAnchor - anchor else anchor - lastAnchor
            if (lastAnchor == Float.MAX_VALUE || distance > g.cell * 2.5f) {
                canvas.drawText(monthNames[m.coerceIn(0, 11)], anchor, y, monthPaint)
                lastAnchor = anchor
            }
        }
    }

    /** Sparse Arabic weekday labels on the reading-start edge, vertically
     *  centered on their rows — mapped by dow, so they cannot mirror into
     *  nonsense in RTL. */
    private fun drawWeekdayLabels(canvas: Canvas, g: Grid) {
        dowPaint.textSize = g.dowSize
        dowPaint.color = if (dark) 0xFFA8A093.toInt() else 0xFF8A8272.toInt()
        dowPaint.textAlign = if (g.rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val x = if (g.rtl) width - 4f else 4f
        for (dow in labeledDows) {
            val rowCenter = g.rowY(dow) + g.cell / 2f
            canvas.drawText(dowNames[dow], x, rowCenter + g.dowSize * 0.32f, dowPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val g = grid(width, height)
            val col = g.colAt(event.x)
            val row = g.rowAt(event.y)
            if (col in 0..14 && row in 0..6) {
                val hit = cells.firstOrNull { it.week == g.weekAtCol(col) && it.dow == row }
                if (hit != null) {
                    selectedDay = hit.day
                    invalidate()
                    listener?.invoke(hit.day, hit.count)
                }
            }
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private fun levelOf(count: Int): Int = when {
        count <= 0 -> 0
        max <= 4 -> (count * 4 / max).coerceAtLeast(1)
        else -> (count * 4 / max).coerceAtLeast(1).coerceAtMost(4)
    }

    /** Shared geometry for drawing, the selection ring, and touch
     *  hit-testing — one source of truth, computed from the same inputs
     *  every time, so the three can never disagree. */
    private class Grid(
        val cell: Float,
        val gap: Float,
        val startX: Float,
        val topPad: Float,
        val monthSize: Float,
        val dowSize: Float,
        val rtl: Boolean
    ) {
        val monthBaseline = topPad - monthSize * 0.35f
        fun colX(week: Int) = startX + (if (rtl) 14 - week else week) * (cell + gap)
        fun rowY(dow: Int) = topPad + dow * (cell + gap)
        fun colAt(x: Float) = ((x - startX) / (cell + gap)).toInt()
        fun rowAt(y: Float) = ((y - topPad) / (cell + gap)).toInt()
        fun weekAtCol(col: Int) = if (rtl) 14 - col else col
    }

    private fun grid(w: Int, h: Int): Grid {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val gap = (w * 0.011f).coerceIn(3f, 10f)
        val dowSize = sp(9f)
        val monthSize = sp(10f)
        // Strip reserved on the reading-start side for weekday labels.
        val stripW = (w * 0.14f).coerceAtMost(dowSize * 3.6f)
        val areaW = (w - stripW).coerceAtLeast(60f)
        val topPad = monthSize * 2.1f
        var cell = ((areaW - gap * 15) / 15f).coerceAtLeast(4f)
        cell = cell.coerceAtMost(((h - topPad - gap * 6) / 7f).coerceAtLeast(4f))
        val gridW = cell * 15 + gap * 14
        val areaLeft = if (rtl) 0f else stripW
        val startX = areaLeft + (areaW - gridW).coerceAtLeast(0f) / 2f
        return Grid(cell, gap, startX, topPad, monthSize, dowSize, rtl)
    }

    private fun sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
}
