package io.vinicius.rmd.model

import com.squareup.moshi.Json

data class Submission(
    val data: Data
) {
    data class Data(
        val domain: String,
        val url: String,

        @Json(name = "post_hint")
        val postHint: String?,

        @Json(name = "created_utc")
        val created: Long,
    )

    override fun equals(other: Any?): Boolean = other is Submission && data.url == other.data.url

    override fun hashCode(): Int = data.url.hashCode()
}
