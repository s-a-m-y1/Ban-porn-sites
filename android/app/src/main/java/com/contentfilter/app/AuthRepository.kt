package com.contentfilter.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(val id: String, val name: String, val email: String, val phone: String)

object AuthRepository {
    private const val PREFS = "auth_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER = "user_json"

    fun isLoggedIn(ctx: Context): Boolean = token(ctx) != null

    fun token(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun user(ctx: Context): AuthUser? {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, null) ?: return null
        return try {
            val o = JSONObject(json)
            AuthUser(o.getString("id"), o.getString("name"), o.getString("email"), o.getString("phone"))
        } catch (_: Exception) { null }
    }

    fun saveSession(ctx: Context, token: String, user: AuthUser) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, JSONObject().apply {
                put("id", user.id); put("name", user.name); put("email", user.email); put("phone", user.phone)
            }.toString()).apply()
    }

    fun logout(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun baseUrl(ctx: Context): String =
        ctx.getSharedPreferences("blocklist_prefs", Context.MODE_PRIVATE).getString("backend_url", "http://10.0.2.2:3000") ?: "http://10.0.2.2:3000"

    suspend fun signup(ctx: Context, name: String, email: String, phone: String, password: String, confirm: String): Result<Pair<AuthUser, String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl(ctx)}/api/auth/signup")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; setRequestProperty("Content-Type", "application/json"); doOutput = true; connectTimeout = 8000; readTimeout = 8000
            }
            val body = JSONObject().apply { put("name", name); put("email", email); put("phone", phone); put("password", password); put("confirmPassword", confirm) }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: ""
            if (code in 200..299) {
                val o = JSONObject(resp)
                val u = o.getJSONObject("user")
                val user = AuthUser(u.getString("id"), u.getString("name"), u.getString("email"), u.getString("phone"))
                val token = o.getString("token")
                Result.success(user to token)
            } else {
                val msg = try { JSONObject(resp).optString("message", resp) } catch (_: Exception) { resp.ifEmpty { "Signup failed" } }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun login(ctx: Context, email: String, password: String): Result<Pair<AuthUser, String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl(ctx)}/api/auth/login")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; setRequestProperty("Content-Type", "application/json"); doOutput = true; connectTimeout = 8000; readTimeout = 8000
            }
            val body = JSONObject().apply { put("email", email); put("password", password) }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: ""
            if (code in 200..299) {
                val o = JSONObject(resp)
                val u = o.getJSONObject("user")
                val user = AuthUser(u.getString("id"), u.getString("name"), u.getString("email"), u.getString("phone"))
                val token = o.getString("token")
                Result.success(user to token)
            } else {
                val msg = try { JSONObject(resp).optString("message", resp) } catch (_: Exception) { resp.ifEmpty { "Login failed" } }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}
