package io.vinicius.rmd.util

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.use
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class Fetch {
    @PublishedApi
    internal val client = HttpClient(Curl) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
    }

    inline fun <reified T> get(url: String): T {
        return client.use {
            runBlocking {
                val response = it.get(url)
                return@runBlocking response.body<T>()
            }
        }
    }
}