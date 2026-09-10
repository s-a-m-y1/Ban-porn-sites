package com.contentfilter.app

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("api/blocklist")
    suspend fun getBlocklist(): List<String>

    @GET("api/blocklist/version")
    suspend fun getVersion(): String

    @GET("api/blocklist/diff")
    suspend fun getDiff(@Query("since") since: String): List<String>

    @POST("api/stats/blocked")
    suspend fun reportBlocked(@Body dto: ReportBlockedDto): retrofit2.Response<Void>

    companion object {
        fun create(baseUrl: String): ApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
                .create(ApiService::class.java)
        }
    }
}
