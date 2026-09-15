package com.contentfilter.app

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN stored as PBKDF2-HMAC-SHA256 hash — never plaintext.
 * V2: 100k iterations + per-install SecureRandom salt.
 * Legacy SHA-256 entries are still verified for migration, then upgraded on next save.
 */
object PinManager {

    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val PREF_FAILED_ATTEMPTS = "pin_failed_attempts"
    private const val PREF_LOCKOUT_UNTIL = "pin_lockout_until"
    private const val MAX_ATTEMPTS = 5
    private const val BASE_LOCKOUT_MS = 30_000L // 30s, doubles each 5 fails

    fun hash(context: Context, pin: String): String {
        val salt = getDeviceSalt(context)
        return pbkdf2(pin, salt, PBKDF2_ITERATIONS)
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val salt = getDeviceSalt(context)
        val hash = pbkdf2(pin, salt, PBKDF2_ITERATIONS)
        val stored = "pbkdf2:$PBKDF2_ITERATIONS:$salt:$hash"
        prefs.edit().putString("pin_hash", stored).apply()
        prefs.edit().remove("pin").apply()
        clearFailedAttempts(context)
    }

    fun verify(context: Context, entered: String): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val stored = prefs.getString("pin_hash", null) ?: return false

        // New format: pbkdf2:iterations:salt:hash
        if (stored.startsWith("pbkdf2:")) {
            val parts = stored.split(":")
            if (parts.size != 4) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val salt = parts[2]
            val expected = parts[3]
            val computed = pbkdf2(entered, salt, iterations)
            return constantTimeEquals(expected, computed)
        }

        // Legacy fallback: single SHA-256(salt+pin) hex
        val legacySalt = getDeviceSalt(context)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((legacySalt + entered).toByteArray(Charsets.UTF_8))
        val legacyHash = digest.joinToString("") { "%02x".format(it) }
        val ok = constantTimeEquals(stored, legacyHash)
        // Opportunistic upgrade on successful legacy verify
        if (ok) {
            savePin(context, entered)
        }
        return ok
    }

    fun isPinSet(context: Context): Boolean =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("pin_hash", null) != null

    /** validation rules: 4–6 digits, not all-same, not a simple sequence */
    fun validate(pin: String): Int {
        if (pin.length !in 4..6) return R.string.pin_err_length
        if (!pin.all { it.isDigit() }) return R.string.pin_err_digits
        if (pin.all { it == pin[0] }) return R.string.pin_err_same
        val nums = pin.map { it - '0' }
        val ascending = nums.zipWithNext().all { (a, b) -> b == a + 1 }
        val descending = nums.zipWithNext().all { (a, b) -> b == a - 1 }
        if (ascending || descending) return R.string.pin_err_seq
        if (pin in setOf("1234", "0000", "1111", "1212", "6969", "1004", "2000", "2020", "1122")) {
            return R.string.pin_err_common
        }
        return 0 // valid
    }

    fun isLockedOut(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val until = prefs.getLong(PREF_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < until
    }

    fun remainingLockoutMs(context: Context): Long {
        val until = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getLong(PREF_LOCKOUT_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun recordFailedAttempt(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val attempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
        prefs.edit().putInt(PREF_FAILED_ATTEMPTS, attempts).apply()
        if (attempts % MAX_ATTEMPTS == 0) {
            val k = attempts / MAX_ATTEMPTS
            val lockMs = BASE_LOCKOUT_MS * (1 shl (k - 1)) // 30s, 60s, 120s...
            prefs.edit().putLong(PREF_LOCKOUT_UNTIL, System.currentTimeMillis() + lockMs).apply()
        }
    }

    fun clearFailedAttempts(context: Context) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().remove(PREF_FAILED_ATTEMPTS).remove(PREF_LOCKOUT_UNTIL).apply()
    }

    /** per-install random salt (hex, 16 bytes) */
    private fun getDeviceSalt(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.getString("pin_salt", null)?.let { return it }
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val salt = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString("pin_salt", salt).apply()
        return salt
    }

    private fun pbkdf2(pin: String, saltHex: String, iterations: Int): String {
        val salt = hexToBytes(saltHex) ?: saltHex.toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        } catch (_: Exception) {
            null
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
