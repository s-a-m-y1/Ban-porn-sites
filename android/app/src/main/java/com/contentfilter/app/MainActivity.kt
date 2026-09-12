package com.contentfilter.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("lang", Locale.getDefault().language.takeIf { it == "ar" } ?: "en")
            ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // theme mode: system | light | dark
        when (prefs().getString("theme_mode", "system")) {
            "dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
            )
            "light" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
            )
        }

        setContentView(R.layout.activity_main)

        val nav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNav,
        )
        if (savedInstanceState == null) {
            switchTo(HomeFragment(), R.id.navHome)
        }
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> switchTo(HomeFragment(), item.itemId)
                R.id.navStats -> switchTo(StatsFragment(), item.itemId)
                R.id.navFeatures -> switchTo(FeaturesFragment(), item.itemId)
                R.id.navSettings -> switchTo(SettingsFragment(), item.itemId)
                R.id.navControl -> switchTo(ControlFragment(), item.itemId)
            }
            true
        }
    }

    private fun switchTo(fragment: Fragment, itemId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun prefs() = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    override fun onResume() {
        super.onResume()
        // re-apply schedule whenever user returns to the app
        ScheduleManager.apply(this)
    }
}
