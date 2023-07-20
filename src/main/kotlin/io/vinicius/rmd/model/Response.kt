package io.vinicius.rmd.model

data class Response(
    val data: Data
) {
    data class Data(
        val after: String?,
        val children: List<Submission>
    )
}