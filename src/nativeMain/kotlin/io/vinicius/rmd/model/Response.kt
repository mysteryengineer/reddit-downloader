package io.vinicius.rmd.model

import kotlinx.serialization.Serializable

@Serializable
data class Response(
    val data: Data
) {
    @Serializable
    data class Data(
        val after: String?,
        val children: List<Submission>
    )
}