package io.vinicius.rmd

import io.vinicius.rmd.model.Download
import io.vinicius.rmd.model.Response
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.util.Fetch
import io.vinicius.rmd.util.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

    if (user == null || limit == null) {
        error("🧨 Missing the environment variable RMD_USER or RMD_LIMIT")
    }

    val submissions = getSubmissions(user, limit)
    val downloads = downloadMedia(user, submissions)

    removeDuplicates(user, downloads)
    createReport(user, downloads)

    println("\n🌟 Done!")
}

private fun getSubmissions(user: String, limit: Int): Set<Submission> {
    val fetch = Fetch()
    val submissions = mutableSetOf<Submission>()
    var before = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
    var counter = 0

    print("\n📝 Collecting $limit posts from user [$user] ")

    do {
        val url = createUrl(user, before)
        val list = fetch.get<Response>(url).data
        submissions.addAll(list)

        // Getting the last "before"
        before = list.lastOrNull()?.created ?: 0
        counter++

        runBlocking {
            print(".")
            delay(2.seconds)
        }
    } while (list.isNotEmpty() && counter < ceil(limit / 250f))

    println(" ${min(submissions.size, limit)}/$limit unique posts found\n")
    return submissions.take(limit).toSet()
}

private fun createUrl(user: String, before: Long): String {
    val baseUrl = "https://api.pushshift.io/reddit/submission/search"
    val fields = "author,created_utc,domain,post_hint,url"
    return "${baseUrl}?author=${user}&fields=${fields}&before=${before}&size=250"
}

private fun downloadMedia(user: String, submissions: Set<Submission>): List<Download> {
    val downloads = mutableListOf<Download>()
    val shell = Shell(File("/tmp/rmd/$user"))
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    val baseFile = "${LocalDateTime.now().format(formatter)}-$user"
    val padding = submissions.size.toString().count()
    var fileName: String

    submissions.forEachIndexed { index, submission ->
        val number = (index + 1).toString().padStart(padding, '0')

        val result = if (submission.postHint == "image") {
            fileName = "$baseFile-$number.jpg"
            print("📸 [$number] Downloading image ${submission.url.take(68)} ".padEnd(100, '.'))
            shell.downloadImage(submission.url, fileName)
        } else {
            fileName = "$baseFile-$number.mp4"
            print("📹 [$number] Downloading video ${submission.url.take(68)} ".padEnd(100, '.'))
            shell.downloadVideo(submission.url, fileName)
        }

        // Add to list of downloads
        val hash = shell.calculateHash(fileName).orEmpty()

        downloads.add(Download(
            url = submission.url,
            fileName = fileName,
            output = result.getOrNull() ?: result.exceptionOrNull()?.message ?: "<Nothing>",
            isSuccess = result.isSuccess,
            hash = hash
        ))

        println(if (result.isSuccess) " ✅ [Success]" else " ❌ [Failure]")
    }

    return downloads
}

private fun removeDuplicates(user: String, downloads: List<Download>) {
    val shell = Shell(File("/tmp/rmd/$user"))

    println("\n🚮 Removing duplicated downloads...")

    // Removing 0-byte files
    downloads.forEach {
        val file = File("/tmp/rmd/$user", it.fileName)
        if (file.exists() && file.length() == 0L) {
            println("[Z] ${file.absoluteFile}")
            file.delete()
        }
    }

    // Removing duplicates
    downloads.groupBy { it.hash }.values.forEach {
        it.drop(1).forEach { download ->
            val file = File("/tmp/rmd/$user", download.fileName)
            if (file.exists()) {
                println("[D] ${file.absoluteFile}")
                file.delete()
            }
        }
    }

    // Removing similar
    val similars = shell.findSimilarImages()
    similars.forEach {
        it.drop(1).forEach { similar ->
            val file = File(similar)
            if (file.exists()) {
                println("[S] ${file.absoluteFile}")
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