package io.vinicius.rmd.model

data class Download(
    val url: String,
    val fileName: String,
    val output: String,
    val isSuccess: Boolean,
    val hash: String
)