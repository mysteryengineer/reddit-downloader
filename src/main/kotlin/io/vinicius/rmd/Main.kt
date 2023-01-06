package io.vinicius.rmd

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import io.vinicius.rmd.model.Download
import io.vinicius.rmd.model.Response
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.util.Fetch
import io.vinicius.rmd.util.Shell
import io.vinicius.rmd.util.Shell.Companion.t
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    val user: String? = System.getenv("RMD_USER")
    val limit: Int? = System.getenv("RMD_LIMIT")?.toInt()
    val parallel: Int = System.getenv("RMD_PARALLEL")?.toInt() ?: 5
    val similar: String? = System.getenv("RMD_SIMILAR")?.uppercase()

    if (user == null || limit == null) {
        error("🧨 Missing the environment variable RMD_USER or RMD_LIMIT")
    }

    val submissions = try {
        getSubmissions(user, limit)
    } catch (ex: Exception) {
        error("\n🧨 Error fetching the posts; check if the servers are up by accessing the site " +
                "https://stats.uptimerobot.com/l8RZDu1gBG")
    }

    val downloads = downloadMedia(user, submissions, parallel)

    removeDuplicates(user, downloads, similar)
    createReport(user, downloads)

    t.println("\n🌟 Done!")
}

private fun getSubmissions(user: String, limit: Int): Set<Submission> {
    val fetch = Fetch()
    val submissions = mutableSetOf<Submission>()
    var before = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
    var counter = 0

    print("\n📝 Collecting ${bold(limit.toString())} posts from user ${bold(user)} ")

    do {
        val url = createUrl(user, before)
        val list = fetch.get<Response>(url).data
        submissions.addAll(list)

        // Getting the last "before"
        before = list.lastOrNull()?.created ?: 0
        counter++

        runBlocking {
            t.print(".")
            delay(2.seconds)
        }
    } while (list.isNotEmpty() && counter < ceil(limit / 1000f))

    t.println(" ${min(submissions.size, limit)}/$limit unique posts found")
    t.println()

    return submissions.take(limit).toSet()
}

private fun createUrl(user: String, until: Long): String {
    // Docs: https://api.pushshift.io/docs
    val baseUrl = "https://api.pushshift.io/reddit/submission/search"
    val filter = "author,created_utc,domain,post_hint,url"
    return "${baseUrl}?author=${user}&filter=${filter}&until=${until}&limit=1000"
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun downloadMedia(user: String, submissions: Set<Submission>, parallel: Int): List<Download> {
    val downloads = mutableListOf<Download>()
    val shell = Shell(File("/tmp/rmd/$user"))
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    val baseFile = "${LocalDateTime.now().format(formatter)}-$user"
    val padding = submissions.size.toString().count()
    val parallelismContext = Dispatchers.IO.limitedParallelism(parallel)

    runBlocking {
        val jobs = submissions.mapIndexed { index, submission ->
            launch(parallelismContext) {
                val fileName: String
                val number = (index + 1).toString().padStart(padding, '0')

                val result = if (submission.postHint == "image") {
                    printDownloading(index, padding, submission)
                    fileName = "$baseFile-$number.jpg"
                    shell.downloadImage(submission.url, fileName)
                } else {
                    printDownloading(index, padding, submission)
                    fileName = "$baseFile-$number.mp4"
                    shell.downloadVideo(submission.url, fileName)
                }

                // Add to list of downloads
                val hash = shell.calculateHash(fileName).orEmpty()

                downloads.add(
                    Download(
                        url = submission.url,
                        fileName = fileName,
                        output = result.getOrNull() ?: result.exceptionOrNull()?.message ?: "<Nothing>",
                        isSuccess = result.isSuccess,
                        hash = hash
                    )
                )

//                t.println(
//                    if (result.isSuccess)
//                        " ✅ ${bold(green("Success"))}"
//                    else
//                        " ❌ ${bold(red("Failure"))}"
//                )
            }
        }

        jobs.joinAll()
    }

    return downloads
}

private fun removeDuplicates(user: String, downloads: List<Download>, option: String?) {
    val shell = Shell(File("/tmp/rmd/$user"))

    t.println("\n🚮 Removing duplicated downloads...")

    // Removing 0-byte files
    downloads.forEach {
        val file = File("/tmp/rmd/$user", it.fileName)
        if (file.exists() && file.length() == 0L) {
            t.println("[${brightBlue("Z")}] ${file.absoluteFile}")
            file.delete()
        }
    }

    // Removing duplicates
    downloads.groupBy { it.hash }.values.forEach {
        it.drop(1).forEach { download ->
            val file = File("/tmp/rmd/$user", download.fileName)
            if (file.exists()) {
                t.println("[${brightRed("D")}] ${file.absoluteFile}")
                file.delete()
            }
        }
    }

    // Removing similar
    val similarImages = if(option == "I" || option == "A") shell.findSimilar("image") else emptyList()
    val similarVideos = if(option == "V" || option == "A") shell.findSimilar("video") else emptyList()
    val similars = similarImages + similarVideos

    similars.forEach {
        it.drop(1).forEach { similar ->
            val file = File(similar)
            if (file.exists()) {
                t.println("[${brightYellow("S")}] ${file.absoluteFile}")
                file.delete()
            }
        }
    }
}

private fun createReport(user: String, downloads: List<Download>) {
    val totalFailed = downloads.count { !it.isSuccess }

    File("/tmp/rmd/$user", "report.md").printWriter().use { out ->
        out.println("# RMD - Download Report")

        out.println("## Failed Downloads")
        out.println("- Total: $totalFailed")

        downloads
            .filter { !it.isSuccess }
            .forEach {
                out.println("### 🔗 Link: ${it.url} - ❌ **Failure**")
                out.println("### 📝 Output:")
                out.println("```")
                out.println(it.output)
                out.println("```")

                out.println("---")
            }
    }
}

// region - Terminal
fun printDownloading(index: Int, padding: Int, submission: Submission) {
    val emoji: String
    val label: String
    val number = (index + 1).toString().padStart(padding, '0')
    val url = brightCyan(underline(submission.url.take(68)))

    if (submission.postHint == "image") {
        emoji = "📸"
        label = magenta("image")
    } else {
        emoji = "📹"
        label = yellow("video")
    }

    t.println("$emoji [${blue(bold(number))}] Downloading $label $url ".padEnd(141, '.'))
}
// endregion