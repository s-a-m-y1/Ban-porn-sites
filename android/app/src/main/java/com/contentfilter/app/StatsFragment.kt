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

class StatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadReport()
    }

    override fun onResume() {
        super.onResume()
        loadReport()
    }

    private fun loadReport() {
        val view = view ?: return
        lifecycleScope.launch {
            val repo = BlocklistRepository(requireContext())
            val stats = repo.dailyStats(7)
            val total = stats.sumOf { it.blockedCount }
            view.findViewById<TextView>(R.id.reportCount)?.text = total.toString()

            // bar chart
            val bars = view.findViewById<LinearLayout>(R.id.reportBars) ?: return@launch
            bars.removeAllViews()
            val max = (stats.maxOfOrNull { it.blockedCount } ?: 0).coerceAtLeast(1)
            if (stats.isNotEmpty()) {
                for (day in stats.reversed()) {
                    val bar = layoutInflater.inflate(R.layout.report_bar, bars, false) as LinearLayout
                    val fill = bar.findViewById<View>(R.id.barFill)
                    val label = bar.findViewById<TextView>(R.id.barLabel)
                    val h = (day.blockedCount * 100 / max).coerceAtLeast(4)
                    fill.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, h * 3,
                    )
                    label.text = day.day.substring(8)
                    bars.addView(bar)
                }
            }

            // heatmap
            val all = repo.allStats().toMap()
            val heatmap = view.findViewById<HeatmapView>(R.id.heatmap) ?: return@launch
            val isDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            heatmap.setData(all, isDark)
            heatmap.onCellClick { day, count ->
                view.findViewById<TextView>(R.id.heatmapSelected)?.text =
                    getString(R.string.heatmap_day_fmt, day, count)
            }
        }
    }
}
