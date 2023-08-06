package io.vinicius.rmd.model

import com.squareup.moshi.Json
import io.vinicius.rmd.type.MediaType

data class Submission(
    val data: Data
) {
    data class Data(
        val domain: String,
        val url: String,

        @Json(name = "post_hint")
        private val postHint: String?,

        @Json(name = "created_utc")
        val created: Long,
    ) {
        val type = getMediaType(url, postHint)
    }

    override fun equals(other: Any?): Boolean = other is Submission && data.url == other.data.url

    override fun hashCode(): Int = data.url.hashCode()
}

private fun getMediaType(url: String, postHint: String?): MediaType {
    return if (url.endsWith(".jpg", true) || url.endsWith(".jpeg", true)) {
        MediaType.Image
    } else if (url.endsWith(".png", true)) {
        MediaType.Image
    } else if (url.endsWith(".gifv", true) || url.endsWith(".mp4", true)) {
        MediaType.Video
    } else if (url.endsWith(".gif", true)) {
        MediaType.Image
    } else if (postHint == "image") {
        MediaType.Image
    } else {
        MediaType.Video
    }
}