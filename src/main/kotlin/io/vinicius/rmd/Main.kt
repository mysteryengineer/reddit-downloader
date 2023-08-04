package io.vinicius.rmd

import io.vinicius.rmd.helper.Shell.Companion.t
import io.vinicius.rmd.util.convertGifs
import io.vinicius.rmd.util.createReport
import io.vinicius.rmd.util.downloadMedia
import io.vinicius.rmd.util.getSubmissions
import io.vinicius.rmd.util.removeDuplicates

fun main() {
    val user = System.getenv("RMD_USER")
    val limit = System.getenv("RMD_LIMIT")?.toInt() ?: 1000
    val parallel = System.getenv("RMD_PARALLEL")?.toInt() ?: 5
    val convert = System.getenv("RMD_CONVERT_GIFS")?.toBoolean() ?: true
    val telemetry = System.getenv("RMD_TELEMETRY")?.toBoolean() ?: true

    if (user == null) {
        error("🧨 Missing the environment variable RMD_USER")
    }

    val submissions = try {
        getSubmissions(user, limit)
    } catch (_: Exception) {
        error("\n🧨 Error fetching the posts; check if the servers are up by accessing the site " +
                "https://www.redditstatus.com")
    }

    val downloads = downloadMedia(user, submissions, parallel)

    removeDuplicates(user, downloads)

    createReport(user, downloads)

    if (convert) convertGifs(user, downloads)

    t.println("\n🌟 Done!")
}