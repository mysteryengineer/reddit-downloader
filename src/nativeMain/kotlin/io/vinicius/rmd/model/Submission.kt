package io.vinicius.rmd.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Submission(
    val data: Data
) {
    @Serializable
    data class Data(
        val domain: String,
        val url: String,

        @SerialName("post_hint")
        val postHint: String? = null,

        @SerialName("created_utc")
        val created: Double,
    )

    override fun equals(other: Any?): Boolean = other is Submission && data.url == other.data.url

    override fun hashCode(): Int = data.url.hashCode()
}