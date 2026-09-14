package com.contentfilter.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.PathInterpolator
import android.view.animation.OvershootInterpolator
import java.util.WeakHashMap

/**
 * Central place for reusable UI animations.
 *
 * Duration scale (ms): 150 press-in / 250 release / 400 entrance.
 * Every helper is safe to call twice on the same view — it restarts cleanly.
 */
object UiAnim {

    /** Standard material entrance curve — shared, stateless.
     *  PathInterpolator(0.4, 0, 0.2, 1) = the material standard cubic,
     *  framework class only (no extra dependency). */
    private val standard = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    /** Release curve: settles back with a barely-there overshoot. */
    private val springBack = OvershootInterpolator(1.1f)

    /** Count-ups in flight, keyed weakly so finished runs can be collected. */
    private val runningCounts = WeakHashMap<android.widget.TextView, AnimatorSet>()

    /** Soft press: slight shrink on down, gentle spring back on up. */
    fun pressable(vararg views: View) {
        for (v in views) {
            v.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        view.animate().cancel()
                        view.animate().scaleX(0.96f).scaleY(0.96f)
                            .setDuration(150)
                            .setInterpolator(standard)
                            .start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        view.animate().cancel()
                        view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(250)
                            .setInterpolator(springBack)
                            .start()
                    }
                }
                false // don't consume — let click still fire
            }
        }
    }

    /** Staggered card entrance: rise + fade, each view one beat after the last. */
    fun staggeredEntrance(vararg views: View, startDelayMs: Long = 60) {
        views.forEachIndexed { i, v ->
            v.animate().cancel() // restart cleanly if called twice
            v.alpha = 0f
            v.translationY = 60f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(i * startDelayMs)
                .setInterpolator(standard)
                .start()
        }
    }

    /**
     * Plain delayed fade-in (splash beats, secondary labels).
     * Durations ride the same 150/250/400 scale; no hidden clamping.
     */
    fun fadeIn(view: View, startDelayMs: Long = 0L, durationMs: Long = 400L) {
        view.animate().cancel()
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setStartDelay(startDelayMs)
            .setDuration(durationMs)
            .setInterpolator(standard)
            .start()
    }

    /** Breathing halo: gentle infinite scale pulse (for the power ring). */
    fun breathe(view: View, active: Boolean) {
        view.clearAnimation()
        if (active) {
            // pulse.xml owns the loop: reverse mode + infinite count = smooth
            // inhale/exhale. Nothing is overridden here on purpose.
            val a = AnimationUtils.loadAnimation(view.context, R.anim.pulse)
            view.startAnimation(a)
        }
    }

    /** Count-up number animation with a slight settle-stretch at the end. */
    fun countUp(view: android.widget.TextView, target: Int, dur: Long = 800) {
        runningCounts.remove(view)?.cancel() // twice-safe: kill any run in flight
        view.scaleX = 1f
        if (target <= 0) {
            view.text = "0"
            return
        }
        val anim = ObjectAnimator.ofInt(0, target).apply {
            duration = dur
            interpolator = standard
            addUpdateListener {
                view.text = (it.animatedValue as Int).toString()
            }
        }
        val bounceX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.12f, 1f).apply {
            duration = 250
            startDelay = dur
        }
        val set = AnimatorSet().apply {
            play(anim).before(bounceX)
            // drop the guard entry when the run ends (finish OR cancel) so the
            // map never holds a finished set strongly referencing its TextView
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    runningCounts.remove(view)
                }
            })
        }
        runningCounts[view] = set
        set.start()
    }
}
