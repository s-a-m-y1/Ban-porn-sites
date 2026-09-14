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

    private fun setUiState(active: Boolean, animated: Boolean) {
        val view = view ?: return
        val powerButton = view.findViewById<ImageButton>(R.id.powerButton) ?: return
        val powerHalo = view.findViewById<View>(R.id.powerHalo)
        val statusText = view.findViewById<TextView>(R.id.statusText)
        val statusSubtitle = view.findViewById<TextView>(R.id.statusSubtitle)
        val statusDot = view.findViewById<View>(R.id.statusDot)
        val powerLabel = view.findViewById<TextView>(R.id.powerLabel)

        vpnActive = active
        // gate face sits flat on ink; the layered f2 halo carries the accent
        powerHalo.setBackgroundResource(
            if (active) R.drawable.f2_halo_on else R.drawable.f2_halo_off,
        )
        if (animated) {
            val punchX = android.animation.ObjectAnimator.ofFloat(powerButton, "scaleX", 1f, 0.96f, 1f)
            val punchY = android.animation.ObjectAnimator.ofFloat(powerButton, "scaleY", 1f, 0.96f, 1f)
            android.animation.AnimatorSet().apply {
                playTogether(punchX, punchY); duration = 350; start()
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
        powerButton.contentDescription = powerLabel.text
        if (animated) {
            val fade = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            statusText.startAnimation(fade)
            statusSubtitle.startAnimation(fade)
        }
    }

    /** time-of-day greeting, personalised with the name chosen during onboarding */
    private fun bindGreeting() {
        val greetingText = view?.findViewById<TextView>(R.id.greetingText) ?: return
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetRes = when (hour) {
            in 4..11 -> R.string.greet_morning
            in 12..16 -> R.string.greet_afternoon
            in 17..20 -> R.string.greet_evening
            else -> R.string.greet_night
        }
        val name = prefs.getString("user_name", null)?.trim().orEmpty()
        val isAr = prefs.getString("lang", "ar") == "ar"
        val greeting = getString(greetRes)
        greetingText.text = when {
            name.isEmpty() -> greeting
            isAr -> "$greeting يا $name"
            else -> "$greeting, $name"
        }
    }

    /** "Day N of your journey" — counted from the onboarding start day */
    private fun bindJourneyChip() {
        val chip = view?.findViewById<TextView>(R.id.journeyChip) ?: return
        val firstDay = prefs.getString("first_open_day", null) ?: return
        val days = try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val start = fmt.parse(firstDay) ?: return
            val today = fmt.parse(fmt.format(java.util.Date()))!!
            ((today.time - start.time) / 86_400_000L).toInt() + 1
        } catch (_: Exception) {
            return
        }
        if (days < 1) return
        chip.text = getString(R.string.journey_day_fmt, days)
        chip.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val powerButton = view.findViewById<ImageButton>(R.id.powerButton)
        val blockedCountText = view.findViewById<TextView>(R.id.blockedCountText)
        val statsText = view.findViewById<TextView>(R.id.statsText)

        // restore state
        vpnActive = prefs.getBoolean("vpn_running", false)
        setUiState(vpnActive, animated = false)

        UiAnim.pressable(powerButton, view.findViewById(R.id.emailButton))
        // whole-page entrance: hero leads, every section follows in one staggered rhythm
        val sections = listOfNotNull(
            view.findViewById<LinearLayout>(R.id.heroContainer),
            view.findViewById<LinearLayout>(R.id.hadithCard),
            view.findViewById<LinearLayout>(R.id.statsCard),
            view.findViewById<LinearLayout>(R.id.stepsCard),
            view.findViewById<LinearLayout>(R.id.trustCard),
            view.findViewById<LinearLayout>(R.id.footerCard),
        )
        if (sections.isNotEmpty()) UiAnim.staggeredEntrance(*sections.toTypedArray())

        bindGreeting()
        bindJourneyChip()

        // rotating hadith card — new one every open; the pool is Arabic in both locales
        val hadithText = view.findViewById<TextView>(R.id.hadithText)
        val pool = resources.getStringArray(R.array.hadiths_ar)
        val idx = System.currentTimeMillis().toInt().mod(pool.size)
        hadithText.text = pool[idx]
        hadithText.alpha = 0f
        hadithText.animate().alpha(1f).setDuration(600).setStartDelay(500).start()

        lifecycleScope.launch {
            val repo = BlocklistRepository(requireContext())
            repo.ensureInitialBlocklist()
            SyncWorker.schedule(requireContext())
            UiAnim.countUp(statsText, repo.count())
            blockedCountText.text = prefs.getInt("blocked_total", 0).toString()
        }

        powerButton.setOnClickListener {
            // a tiny pulse under the finger — the toggle should feel physical
            powerButton.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
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
                askOverlayPermissionIfNeeded()
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
        if (requestCode == 3 && resultCode == android.app.Activity.RESULT_OK) {
            if (!prefs.getBoolean("vpn_running", false)) return
            FilterVpnService.stop(requireContext())
            prefs.edit().putBoolean("vpn_running", false).apply()
            setUiState(false, animated = true)
        }
        if (requestCode == 1 && resultCode == android.app.Activity.RESULT_OK) {
            FilterVpnService.start(requireContext())
            prefs.edit().putBoolean("vpn_running", true).apply()
            setUiState(true, animated = true)
            askOverlayPermissionIfNeeded()
        }
    }

    /**
     * The blocked-site screen needs "display over other apps" (Android 14+
     * blocks service-launched activities otherwise). Ask once, quietly, the
     * first time protection is switched on.
     */
    private fun askOverlayPermissionIfNeeded() {
        if (android.provider.Settings.canDrawOverlays(requireContext())) return
        if (prefs.getBoolean("overlay_asked", false)) return
        prefs.edit().putBoolean("overlay_asked", true).apply()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage(R.string.blockscreen_need_overlay)
            .setPositiveButton(R.string.grant) { _, _ ->
                startActivityForResult(
                    android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${requireContext().packageName}"),
                    ), 8,
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.blockedCountText)?.text =
            prefs.getInt("blocked_total", 0).toString()
        // sync UI with external state changes (e.g. schedule turned it off)
        val running = prefs.getBoolean("vpn_running", false)
        if (running != vpnActive) {
            setUiState(running, animated = false)
        }
    }
}
