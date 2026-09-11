package com.contentfilter.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils

/**
 * Central place for reusable UI animations.
 */
object UiAnim {

    /** Soft press: slight shrink on down, spring back on up. */
    fun pressable(vararg views: View) {
        for (v in views) {
            v.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        view.animate().scaleX(0.96f).scaleY(0.96f)
                            .setDuration(90).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(140).start()
                    }
                }
                false // don't consume — let click still fire
            }
        }
    }

    /** Staggered card entrance: slide+fade each card one after another. */
    fun staggeredEntrance(vararg views: View, startDelayMs: Long = 60) {
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 60f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setStartDelay(i * startDelayMs)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.2f))
                .start()
        }
    }

    /** Breathing halo: gentle infinite scale pulse (for the power ring). */
    fun breathe(view: View, active: Boolean) {
        view.clearAnimation()
        if (active) {
            val a = AnimationUtils.loadAnimation(view.context, R.anim.pulse)
            a.repeatMode = Animation.RESTART
            view.startAnimation(a)
        }
    }

    /** Count-up number animation with slight bounce at the end. */
    fun countUp(view: android.widget.TextView, target: Int, dur: Long = 800) {
        if (target <= 0) {
            view.text = "0"
            return
        }
        val anim = ObjectAnimator.ofInt(0, target).apply {
            duration = dur
            addUpdateListener {
                view.text = (it.animatedValue as Int).toString()
            }
        }
        val bounceX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.12f, 1f).apply {
            duration = 260
            startDelay = dur
        }
        AnimatorSet().apply {
            play(anim).before(bounceX)
            start()
        }
    }
}
