package com.contentfilter.app

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Stats screen: three stat cards (weekly total / today / daily average),
 * a 7-day bar report, and the 15-week blocking heatmap.
 *
 * UI-layer only — data calls are unchanged. The old header counter
 * (reportCount) no longer exists in the layout: the weekly total lives in
 * the first stat card, so this binds statTotalValue instead.
 */
class StatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadReport()
    }

    // MainActivity shows/hides fragments, so every tab return fires onResume.
    // Reload quietly — no entrance animation — so numbers are always fresh.
    override fun onResume() {
        super.onResume()
        loadReport()
    }

    private fun loadReport() {
        val view = view ?: return
        lifecycleScope.launch {
            val repo = BlocklistRepository(requireContext())
            val stats = repo.dailyStats(7)              // newest-first
            val total = stats.sumOf { it.blockedCount }

            // --- Stat cards row ---
            // Total animates up (countUp shows "0" for an empty week); today
            // and the 7-day average render as plain LTR numbers.
            view.findViewById<TextView>(R.id.statTotalValue)?.let { UiAnim.countUp(it, total) }
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(System.currentTimeMillis())
            val todayCount = stats.firstOrNull { it.day == todayKey }?.blockedCount ?: 0
            view.findViewById<TextView>(R.id.statTodayValue)?.text = todayCount.toString()
            view.findViewById<TextView>(R.id.statAverageValue)?.text = (total / 7).toString()

            // --- Weekly bar report ---
            val bars = view.findViewById<LinearLayout>(R.id.reportBars) ?: return@launch
            bars.removeAllViews()
            val max = (stats.maxOfOrNull { it.blockedCount } ?: 0).coerceAtLeast(1)
            for (day in stats.reversed()) {            // oldest-first
                val bar = layoutInflater.inflate(R.layout.report_bar, bars, false) as LinearLayout
                val fill = bar.findViewById<View>(R.id.barFill)
                val label = bar.findViewById<TextView>(R.id.barLabel)
                val h = (day.blockedCount * 100 / max).coerceAtLeast(4)
                fill.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, h * 3
                )
                label.text = day.day.substring(8)
                bars.addView(bar)
            }
            view.findViewById<TextView>(R.id.reportEmpty)?.visibility =
                if (total == 0) View.VISIBLE else View.GONE
            // Empty week: show only the designed empty message, not seven
            // minimum-height slivers under it.
            bars.visibility = if (total == 0) View.GONE else View.VISIBLE

            // --- Heatmap (all-time daily counts) ---
            val all = repo.allStats().toMap()
            val heatmap = view.findViewById<HeatmapView>(R.id.heatmap) ?: return@launch
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            heatmap.setData(all, isDark)
            // setData re-selects "today"; keep the readout text in sync.
            view.findViewById<TextView>(R.id.heatmapSelected)?.text =
                getString(R.string.heatmap_today)
            view.findViewById<TextView>(R.id.heatmapEmpty)?.visibility =
                if (all.values.sum() == 0) View.VISIBLE else View.GONE
            heatmap.onCellClick { day, count ->
                view.findViewById<TextView>(R.id.heatmapSelected)?.text =
                    getString(R.string.heatmap_day_fmt, day, count)
            }
        }
    }
}
