package com.contentfilter.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var vpnActive = false
    private val prefs by lazy { requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val powerButton = view.findViewById<ImageButton>(R.id.powerButton)
        val powerHalo = view.findViewById<View>(R.id.powerHalo)
        val statusText = view.findViewById<TextView>(R.id.statusText)
        val statusSubtitle = view.findViewById<TextView>(R.id.statusSubtitle)
        val statusDot = view.findViewById<View>(R.id.statusDot)
        val powerLabel = view.findViewById<TextView>(R.id.powerLabel)
        val blockedCountText = view.findViewById<TextView>(R.id.blockedCountText)
        val statsText = view.findViewById<TextView>(R.id.statsText)

        fun setUiState(active: Boolean, animated: Boolean) {
            vpnActive = active
            powerButton.background = ContextCompat.getDrawable(
                requireContext(), if (active) R.drawable.btn_primary else R.drawable.btn_danger,
            )
            powerHalo.setBackgroundResource(
                if (active) R.drawable.power_bg_on else R.drawable.power_bg_off,
            )
            if (animated) {
                val punchX = android.animation.ObjectAnimator.ofFloat(powerButton, "scaleX", 1f, 0.85f, 1.15f, 1f)
                val punchY = android.animation.ObjectAnimator.ofFloat(powerButton, "scaleY", 1f, 0.85f, 1.15f, 1f)
                android.animation.AnimatorSet().apply {
                    playTogether(punchX, punchY); duration = 450; start()
                }
            }
            statusDot.setBackgroundResource(
                if (active) R.drawable.status_dot_on else R.drawable.status_dot_off,
            )
            UiAnim.breathe(powerHalo, active)
            if (active) statusDot.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.pulse),
            ) else statusDot.clearAnimation()

            statusText.text = getString(if (active) R.string.status_on else R.string.status_off)
            statusSubtitle.text = getString(
                if (active) R.string.status_on_subtitle else R.string.status_off_subtitle,
            )
            powerLabel.text = getString(
                if (active) R.string.stop_blocking else R.string.start_blocking,
            )
            if (animated) {
                val fade = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                statusText.startAnimation(fade)
                statusSubtitle.startAnimation(fade)
            }
        }

        // restore state
        vpnActive = prefs.getBoolean("vpn_running", false)
        setUiState(vpnActive, animated = false)

        UiAnim.pressable(powerButton, view.findViewById(R.id.emailButton))
        // hero + quick stats + contact cards entrance
        val hero = view.findViewById<FrameLayout>(R.id.heroContainer)
        val quickStats = view.findViewById<LinearLayout>(R.id.quickStatsRow)
        val contact = view.findViewById<LinearLayout>(R.id.contactCard)
        if (hero != null && quickStats != null && contact != null) {
            UiAnim.staggeredEntrance(hero, quickStats, contact)
        }

        lifecycleScope.launch {
            val repo = BlocklistRepository(requireContext())
            repo.ensureInitialBlocklist()
            SyncWorker.schedule(requireContext())
            UiAnim.countUp(statsText, repo.count())
            blockedCountText.text = prefs.getInt("blocked_total", 0).toString()
        }

        powerButton.setOnClickListener {
            if (vpnActive) {
                if (prefs.getBoolean("pin_enabled", false)) {
                    startActivityForResult(
                        android.content.Intent(requireContext(), PinActivity::class.java), 3,
                    )
                } else {
                    FilterVpnService.stop(requireContext())
                    prefs.edit().putBoolean("vpn_running", false).apply()
                    setUiState(false, animated = true)
                }
                return@setOnClickListener
            }
            val prepare = android.net.VpnService.prepare(requireContext())
            if (prepare != null) startActivityForResult(prepare, 1) else {
                FilterVpnService.start(requireContext())
                prefs.edit().putBoolean("vpn_running", true).apply()
                setUiState(true, animated = true)
            }
        }

        view.findViewById<Button>(R.id.emailButton).setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:sam858y@gmail.com")
                putExtra(android.content.Intent.EXTRA_SUBJECT, "[Content Filter] Feedback")
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                android.widget.Toast.makeText(
                    requireContext(), "sam858y@gmail.com", android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == android.app.Activity.RESULT_OK) {
            FilterVpnService.start(requireContext())
            prefs.edit().putBoolean("vpn_running", true).apply()
            view?.let {
                it.findViewById<ImageButton>(R.id.powerButton)?.performClick()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.blockedCountText)?.text =
            prefs.getInt("blocked_total", 0).toString()
        // sync UI with external state changes (e.g. schedule turned it off)
        val running = prefs.getBoolean("vpn_running", false)
        if (running != vpnActive) {
            vpnActive = running
            view?.let { v ->
                v.findViewById<ImageButton>(R.id.powerButton)?.performClick()
            }
        }
    }
}
