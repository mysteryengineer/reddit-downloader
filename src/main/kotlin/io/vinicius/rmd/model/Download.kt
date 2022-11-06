package io.vinicius.rmd.model

data class Download(
    val fileName: String,
    val isSuccess: Boolean,
    val hash: String
)