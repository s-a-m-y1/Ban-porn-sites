package com.contentfilter.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val icon = findViewById<ImageView>(R.id.splashIcon)
        val titleAr = findViewById<TextView>(R.id.splashTitleAr)
        val titleEn = findViewById<TextView>(R.id.splashTitleEn)
        val line = findViewById<View>(R.id.splashLine)

        // 1) icon pops in with overshoot
        icon.scaleX = 0f; icon.scaleY = 0f; icon.alpha = 0f
        val popX = ObjectAnimator.ofFloat(icon, "scaleX", 0f, 1.12f, 1f).apply {
            duration = 650; interpolator = DecelerateInterpolator(1.6f)
        }
        val popY = ObjectAnimator.ofFloat(icon, "scaleY", 0f, 1.12f, 1f).apply {
            duration = 650; interpolator = DecelerateInterpolator(1.6f)
        }
        val fadeIn = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f).apply { duration = 300 }

        // 2) titles slide up + fade
        titleAr.alpha = 0f; titleAr.translationY = 40f
        titleEn.alpha = 0f; titleEn.translationY = 40f
        val arIn = ObjectAnimator.ofFloat(titleAr, "alpha", 0f, 1f).apply { duration = 400 }
        val arUp = ObjectAnimator.ofFloat(titleAr, "translationY", 40f, 0f).apply { duration = 400 }
        val enIn = ObjectAnimator.ofFloat(titleEn, "alpha", 0f, 1f).apply { duration = 400 }
        val enUp = ObjectAnimator.ofFloat(titleEn, "translationY", 40f, 0f).apply { duration = 400 }

        // 3) glow line sweep
        line.scaleX = 0f
        val lineIn = ObjectAnimator.ofFloat(line, "scaleX", 0f, 1f).apply {
            duration = 500; startDelay = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(popX, popY, fadeIn)
            play(arIn).with(arUp).after(250)
            play(enIn).with(enUp).after(400)
            play(lineIn).after(500)
            start()
        }

        // hold splash briefly, then home
        CoroutineScope(Dispatchers.Main).launch {
            delay(1400)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
