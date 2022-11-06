package io.vinicius.rmd.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Submission(
    val author: String,
    val domain: String,
    val url: String,

    @Json(name = "created_utc")
    val created: Long,

    @Json(name = "post_hint")
    val postHint: String?
) {
    override fun equals(other: Any?): Boolean = other is Submission && url == other.url

    override fun hashCode(): Int = url.hashCode()
}