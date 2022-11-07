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
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    val user: String? = System.getenv("RMD_USERR")
    val limit: Int? = System.getenv("RMD_LIMIT")?.toInt()

    if (user == null || limit == null) {
        error("🧨".em + " Missing the environment variable RMD_USER or RMD_LIMIT")
    }

    val submissions = getSubmissions(user, limit)
    val downloads = downloadMedia(user, submissions)

    removeDuplicates(user, downloads)
    println("\n🌟 Done!")
}

private fun getSubmissions(user: String, limit: Int): Set<Submission> {
    val fetch = Fetch()
    val submissions = mutableSetOf<Submission>()
    var before = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

    print("📝".em + " Collecting $limit posts from user [$user] ")

    repeat(ceil(limit / 250f).toInt()) {
        val url = createUrl(user, before)
        val list = fetch.get<Response>(url).data
        submissions.addAll(list)

        // Getting the last "before"
        before = list.last().created

        runBlocking {
            print(".")
            delay(2.seconds)
        }
    }

    println(" $submissions/$limit unique posts found\n")
    return submissions.toSet()
}

private fun createUrl(user: String, before: Long): String {
    val baseUrl = "https://api.pushshift.io/reddit/submission/search"
    val fields = "author,created_utc,domain,post_hint,url"
    return "${baseUrl}?author=${user}&fields=${fields}&before=${before}"
}

private fun downloadMedia(user: String, submissions: Set<Submission>): List<Download> {
    val downloads = mutableListOf<Download>()
    val shell = Shell(File("/tmp/rmd/$user"))
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    val baseFile = "${LocalDateTime.now().format(formatter)}-$user"
    var fileName: String

    submissions.forEachIndexed { index, submission ->
        val number = (index + 1).toString().padStart(5, '0')

        val success = if (submission.postHint == "image") {
            fileName = "$baseFile-$number.jpg"
            print("🖼".em + " [$number] Downloading image ${submission.url} ".padEnd(100, '.'))
            shell.downloadImage(submission.url, fileName)
        } else {
            fileName = "$baseFile-$number.mp4"
            print("🎥".em + " [$number] Downloading video ${submission.url} ".padEnd(100, '.'))
            shell.downloadVideo(submission.url, fileName)
        }

        // Add to list of downloads
        val hash = shell.calculateHash(fileName).orEmpty()
        downloads.add(Download(fileName, success, hash))

        println(if (success) " ✅ [Success]" else " ❌ [Failure]")
    }

    return downloads
}

private fun removeDuplicates(user: String, downloads: List<Download>) {
    println("\n" + "🗑".em + " Removing duplicated downloads...")

    // Removing 0-byte files
    downloads.forEach {
        val file = File("/tmp/rmd/$user", it.fileName)
        if (file.exists() && file.length() == 0L) file.delete()
    }

    // Removing duplicates
    downloads.groupBy { it.hash }.values.forEach {
        it.drop(1).forEach { download ->
            val file = File("/tmp/rmd/$user", download.fileName)
            if (file.exists()) {
                println(file.absoluteFile)
                file.delete()
            }
        }
    }
}