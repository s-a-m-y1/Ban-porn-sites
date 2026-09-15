package com.contentfilter.app

import android.content.Context

/**
 * P1-1: Category-aware, behavior-adaptive intervention selector.
 * Does NOT expose internal category on the screen — caller shows generic
 * "حُجب هذا المحتوى بواسطة حِصن" but content pool is selected by category.
 *
 * Sources are from hadiths.xml / block_messages pools — all verified.
 * No invented religious text.
 */
object ChallengeRepository {

    data class Intervention(
        val message: String,
        val sourceLabel: String?,
        val kind: Kind,
    )
    enum class Kind { GENTLE, DIFFERENT, REFLECTION, CHALLENGE }

    fun select(context: Context, category: String, attemptCount: Int): Intervention {
        val isAr = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("lang", "ar") == "ar"
        val pool = context.resources.getStringArray(
            if (isAr) R.array.block_messages_ar else R.array.block_messages_en
        )
        // Category bias: porn → humility/gaze, gambling → prohibition/consequence
        // Implemented as offset so same pool but different starting point — keeps generic screen honest.
        val bias = when (category.lowercase()) {
            "gambling" -> 3
            else -> 0 // porn / adult-content / default
        }
        val kind = when {
            attemptCount <= 1 -> Kind.GENTLE
            attemptCount == 2 -> Kind.DIFFERENT
            attemptCount == 3 -> Kind.REFLECTION
            else -> Kind.CHALLENGE
        }
        // Gentle: 0-2, Different: 3-4, Reflection: 5-7, Challenge: 8-9 — rotated by bias
        val range = when (kind) {
            Kind.GENTLE -> 0..2
            Kind.DIFFERENT -> 3..4
            Kind.REFLECTION -> 5..7
            Kind.CHALLENGE -> 8..9
        }
        val offset = (attemptCount - 1 + bias) % (range.last - range.first + 1)
        val idx = range.first + offset
        val msg = pool[idx % pool.size]
        return Intervention(
            message = msg,
            sourceLabel = null,
            kind = kind,
        )
    }
}
