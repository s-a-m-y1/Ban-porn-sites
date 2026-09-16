package com.contentfilter.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProgressFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val level = view.findViewById<TextView>(R.id.levelText)
        val xp = view.findViewById<TextView>(R.id.xpText)
        val streak = view.findViewById<TextView>(R.id.streakText)
        lifecycleScope.launch {
            // Placeholder: fetch from /api/progress when logged in, else local
            level.text = "المستوى 1"
            xp.text = "0 / 100 XP"
            streak.text = "—"
        }
    }
}
