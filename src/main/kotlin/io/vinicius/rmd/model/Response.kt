package io.vinicius.rmd.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Response(val data: List<Submission>)