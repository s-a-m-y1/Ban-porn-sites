package com.contentfilter.app

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * Shared base: applies the user's language preference to every activity,
 * not just MainActivity (PIN screens, splash, etc. included).
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("lang", "ar")
            ?: "ar"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
