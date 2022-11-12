package io.vinicius.rmd.util

import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class Fetch {
    val moshi = Moshi.Builder().build()
    val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .build()

    inline fun <reified T> get(url: String): T {
        val adapter = moshi.adapter(T::class.java)
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return adapter.fromJson(response.body!!.source())!!
        }
    }

    inline fun <reified T> getFlow(url: String) = callbackFlow<T> {
        try {
            trySend(get(url))
            close()
        } catch (e: Exception) {
            close(e)
        }

        awaitClose()
    }
}