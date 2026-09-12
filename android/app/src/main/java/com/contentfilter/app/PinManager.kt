package com.contentfilter.app

import android.content.Context
import java.security.MessageDigest

/**
 * PIN stored as salted SHA-256 hash — never plaintext.
 */
object PinManager {

    fun hash(context: Context, pin: String): String {
        val salt = getDeviceSalt(context)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("pin_hash", hash(context, pin)).apply()
        // remove legacy plaintext if present
        prefs.edit().remove("pin").apply()
    }

    fun verify(context: Context, entered: String): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val stored = prefs.getString("pin_hash", null) ?: return false
        return constantTimeEquals(stored, hash(context, entered))
    }

    fun isPinSet(context: Context): Boolean =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("pin_hash", null) != null

    /** per-install random salt (regenerated if wiped) */
    private fun getDeviceSalt(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.getString("pin_salt", null)?.let { return it }
        val salt = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("pin_salt", salt).apply()
        return salt
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
