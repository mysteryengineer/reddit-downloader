package io.vinicius.rmd

import io.vinicius.rmd.model.Response
import io.vinicius.rmd.util.Fetch
import io.vinicius.rmd.util.Shell

fun main() {
    println("Hello, Kotlin/Native!")

    val shell = Shell()
    println(shell.calculateHash())

    val fetch = Fetch()
    val response = fetch.get<Response>("https://www.reddit.com/user/atomicbrunette18/submitted.json?limit=10&sort=new&after=t3_154noit&raw_json=1")
    println(response)
}