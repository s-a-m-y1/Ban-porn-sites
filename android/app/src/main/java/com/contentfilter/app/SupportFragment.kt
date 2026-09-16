package com.contentfilter.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SupportFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_support, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.supportMessage).text = SupportConfig.message
        view.findViewById<TextView>(R.id.supportEmail).text = SupportConfig.email
        view.findViewById<TextView>(R.id.supportEmail).setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${SupportConfig.email}")))
        }
        val paymentsContainer = view.findViewById<LinearLayout>(R.id.paymentsContainer)
        paymentsContainer.removeAllViews()
        var hasEnabled = false
        for (p in SupportConfig.payments) {
            if (!p.enabled) continue
            hasEnabled = true
            val tv = TextView(requireContext()).apply { text = p.name; setPadding(24,16,24,16) }
            tv.setOnClickListener { p.url?.let { u -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } }
            paymentsContainer.addView(tv)
        }
        if (!hasEnabled) {
            val tv = TextView(requireContext()).apply { text = "وسائل الدفع ستُفعّل قريباً — تواصل عبر البريد"; setPadding(24,16,24,16); alpha = 0.7f }
            paymentsContainer.addView(tv)
        }
        view.findViewById<Button>(R.id.btnSupport).text = SupportConfig.cta
        view.findViewById<Button>(R.id.btnSupport).setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${SupportConfig.email}?subject=Support HISN")))
        }
    }
}
