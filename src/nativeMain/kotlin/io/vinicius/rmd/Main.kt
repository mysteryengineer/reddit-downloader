package io.vinicius.rmd

import io.vinicius.rmd.util.Shell

fun main() {
    println("Hello, Kotlin/Native!")

    val shell = Shell()
    println(shell.calculateHash())
}