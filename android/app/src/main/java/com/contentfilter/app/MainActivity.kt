package com.contentfilter.app

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.Locale

class MainActivity : BaseActivity() {

    private var currentTabId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // theme mode: light is the default; system | dark opt-in
        when (prefs().getString("theme_mode", "light")) {
            "dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
            )
            "system" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            )
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
            )
        }

        setContentView(R.layout.activity_main)

        val nav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNav,
        )

        // Belt-and-suspenders with the NavIndicator style: themes.xml now sets
        // the pill to @color/green_12 at the source (F7 integration item). The
        // runtime tint stays so the pill is brand-correct even if the style is
        // bypassed; green_12 auto-resolves per mode (12% Guard Teal light /
        // 20% #8FC4B6 night).
        nav.itemActiveIndicatorColor = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.green_12),
        )

        if (savedInstanceState == null) {
            switchTo(HomeFragment(), R.id.navHome)
        }
        nav.setOnItemSelectedListener { item ->
            switchTo(fragmentFor(item.itemId), item.itemId)
            true
        }
    }

    private fun fragmentFor(itemId: Int): Fragment {
        // reuse the fragment already managed by the FragmentManager
        // (survives activity recreation) instead of stacking new instances
        val tag = "tab_$itemId"
        supportFragmentManager.findFragmentByTag(tag)?.let { return it }
        return when (itemId) {
            R.id.navStats -> StatsFragment()
            R.id.navFeatures -> FeaturesFragment()
            R.id.navSupport -> SupportFragment()
            R.id.navSettings -> SettingsFragment()
            else -> HomeFragment()
        }
    }

    private fun switchTo(fragment: Fragment, itemId: Int) {
        val fm = supportFragmentManager

        fm.fragments.forEach { f ->
            if (f != fragment && !f.isHidden) {
                fm.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .hide(f).commit()
            }
        }

        if (fragment.isAdded) {
            if (fragment.isHidden) fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .show(fragment).commit()
        } else {
            fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .add(R.id.fragmentContainer, fragment, "tab_$itemId")
                .commit()
        }
        currentTabId = itemId
    }

    private fun prefs() = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    override fun onResume() {
        super.onResume()
        // re-apply schedule whenever user returns to the app
        ScheduleManager.apply(this)
        if (!prefs().getBoolean("vpn_running", false)) return

        if (FilterVpnService.needsConsent) {
            // VPN consent was revoked/lost — ask the user again (once per loss)
            android.net.VpnService.prepare(this)?.let {
                startActivityForResult(it, 1)
            }
        } else if (!FilterVpnService.isRunning) {
            // service died silently (force-stop/crash): resume quietly
            FilterVpnService.start(this)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            FilterVpnService.needsConsent = false
            FilterVpnService.start(this)
        }
    }
}
