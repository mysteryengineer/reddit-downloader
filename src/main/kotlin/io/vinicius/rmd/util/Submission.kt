package io.vinicius.rmd.util

import com.github.ajalt.mordant.rendering.TextStyles
import io.vinicius.rmd.helper.Fetch
import io.vinicius.rmd.helper.Shell.Companion.t
import io.vinicius.rmd.model.Response
import io.vinicius.rmd.model.Submission
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

fun getSubmissions(user: String, limit: Int): Set<Submission> {
    val fetch = Fetch()
    val submissions = mutableSetOf<Submission>()
    var after: String? = null
    var counter = 0

    t.print("\n📝 Collecting ${TextStyles.bold(limit.toString())} posts from user ${TextStyles.bold(user)} ")

    do {
        val url = createUrl(user, after ?: "")
        val response = fetch.get<Response>(url).data
        val list = response.children
        submissions.addAll(list)

        // Getting the last "before"
        after = response.after
        counter++

        runBlocking {
            t.print(".")
            delay(2.seconds)
        }
    } while (list.isNotEmpty() && counter < ceil(limit / 100f) && after != null)

    t.println(" ${min(submissions.size, limit)}/$limit unique posts found")

    return submissions.take(limit).toSet()
}