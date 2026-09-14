package com.contentfilter.app

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.animation.DecelerateInterpolator

/**
 * Opening sequence (brand video spec):
 * 0.00s  the pen draws the arch — outer contour, then the inner one (thin line)
 * 1.30s  the drawn outline dissolves into the solid ring (fill crossfade)
 * 2.10s  the arch body fills — the ring becomes the full gate
 * 2.60s  the slit (closed gate) appears last
 * 2.70s  wordmark حِصن rises softly
 * 3.20s  HISN companion label fades in
 * 3.60s  whole composition settles with one quiet zoom-out
 * 4.40s  continue to welcome / home
 */
class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val mark = findViewById<android.view.View>(R.id.splashMark)
        val outline = findViewById<ImageView>(R.id.splashOutline)
        val solid = findViewById<ImageView>(R.id.splashSolid)
        val slit = findViewById<ImageView>(R.id.splashSlit)
        val titleAr = findViewById<TextView>(R.id.splashTitleAr)
        val titleEn = findViewById<TextView>(R.id.splashTitleEn)

        // 1) the pen trace: outer contour, inner contour, then fill crossfade
        val avd = ContextCompat.getDrawable(this, R.drawable.splash_gate_avd) as? AnimatedVectorDrawable
        if (avd != null) {
            outline.setImageDrawable(avd)
            avd.start()
        } else {
            outline.setImageResource(R.drawable.ic_splash_outline)
        }

        // 2) the full gate body fills the ring
        solid.alpha = 0f
        solid.animate()
            .alpha(1f)
            .setStartDelay(2100)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 3) the slit (closed gate) appears last
        slit.alpha = 0f
        slit.animate()
            .alpha(1f)
            .setStartDelay(2600)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 4) wordmark rises
        titleAr.alpha = 0f
        titleAr.translationY = 22f
        titleAr.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(2700)
            .setDuration(700)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 5) HISN label
        titleEn.alpha = 0f
        titleEn.animate()
            .alpha(1f)
            .setStartDelay(3200)
            .setDuration(600)
            .start()

        // 6) one quiet settle: the composition exhales into place
        mark.scaleX = 1.04f
        mark.scaleY = 1.04f
        mark.animate()
            .scaleX(1f).scaleY(1f)
            .setStartDelay(3600)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 7) continue — first launch goes to the welcome/onboarding screen
        CoroutineScope(Dispatchers.Main).launch {
            delay(4400)
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val next = if (prefs.getBoolean("onboarding_done", false)) {
                MainActivity::class.java
            } else {
                WelcomeActivity::class.java
            }
            startActivity(Intent(this@SplashActivity, next))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
