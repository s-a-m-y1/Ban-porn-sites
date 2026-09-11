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
 * Opening sequence (single run, brand spec):
 * 0.00s  arch outline traces itself (pen draw, 1.6s)
 * 1.60s  solid arch body fades in over the traced outline
 * 2.35s  vertical slit (closed gate) fades in last
 * 2.80s  wordmark حِصن rises softly
 * 3.30s  HISN companion label fades in
 * 4.10s  continue to home
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val outline = findViewById<ImageView>(R.id.splashOutline)
        val solid = findViewById<ImageView>(R.id.splashSolid)
        val slit = findViewById<ImageView>(R.id.splashSlit)
        val titleAr = findViewById<TextView>(R.id.splashTitleAr)
        val titleEn = findViewById<TextView>(R.id.splashTitleEn)

        // 1) pen trace
        val avd = ContextCompat.getDrawable(this, R.drawable.splash_gate_avd) as? AnimatedVectorDrawable
        if (avd != null) {
            outline.setImageDrawable(avd)
            avd.start()
        } else {
            outline.setImageResource(R.drawable.ic_splash_outline)
        }

        // 2) solid body settles in after the trace completes
        solid.alpha = 0f
        solid.animate()
            .alpha(1f)
            .setStartDelay(1600)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 3) the slit (closed gate) fades in last
        slit.alpha = 0f
        slit.animate()
            .alpha(1f)
            .setStartDelay(2350)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 4) wordmark rises
        titleAr.alpha = 0f
        titleAr.translationY = 22f
        titleAr.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(2800)
            .setDuration(700)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 5) HISN label
        titleEn.alpha = 0f
        titleEn.animate()
            .alpha(1f)
            .setStartDelay(3300)
            .setDuration(600)
            .start()

        // 6) continue to home
        CoroutineScope(Dispatchers.Main).launch {
            delay(4100)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
