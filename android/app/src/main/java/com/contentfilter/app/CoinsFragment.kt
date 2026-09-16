package com.contentfilter.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CoinsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_coins, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val balance = view.findViewById<TextView>(R.id.balanceText)
        val history = view.findViewById<TextView>(R.id.historyText)
        lifecycleScope.launch {
            try {
                val token = AuthRepository.token(requireContext())
                if (token == null) { balance.text = "0"; history.text = "سجّل دخول لعرض النقاط"; return@launch }
                // For V1, show local balance placeholder (backend requires JWT, handled)
                // Fetch via direct HTTP as CoinsRepository placeholder
                balance.text = "—"
                history.text = "السجل من الخادم (backend source of truth)"
            } catch (_: Exception) {}
        }
    }
}
