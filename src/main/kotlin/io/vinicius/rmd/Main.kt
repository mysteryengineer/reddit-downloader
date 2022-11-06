package io.vinicius.rmd

import io.vinicius.rmd.model.Download
import io.vinicius.rmd.model.Response
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.util.Fetch
import io.vinicius.rmd.util.Shell
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

fun main(args: Array<String>) {
    val user = System.getenv("RMD_USER")
    val limit = System.getenv("RMD_LIMIT").toInt()

    println("⬇ Downloading $limit media from user [$user]...\n")

    val submissions = getSubmissions(user, limit)
    val downloads = downloadMedia(user, submissions)

    cleanFailedDownloads(user, downloads)
    println("\n🌟 Done!")
}

private fun getSubmissions(user: String, limit: Int): Set<Submission> {
    val fetch = Fetch()
    val submissions = mutableListOf<Submission>()
    var before = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

    repeat(ceil(limit / 100f).toInt()) {
        val url = createUrl(user, before)
        val list = fetch.get<Response>(url).data
        submissions.addAll(list)

        // Getting the last "before"
        before = list.last().created
    }

    return submissions.toSet()
}

private fun createUrl(user: String, before: Long): String {
    val baseUrl = "https://api.pushshift.io/reddit/submission/search"
    val fields = "author,created_utc,domain,post_hint,url"
    return "${baseUrl}?author=${user}&fields=${fields}&before=${before}&size=100"
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
            print("🖼 [$number] Downloading image ${submission.url} ".padEnd(100, '.'))
            shell.downloadImage(submission.url, fileName)
        } else {
            fileName = "$baseFile-$number.mp4"
            print("🎥 [$number] Downloading video ${submission.url} ".padEnd(100, '.'))
            shell.downloadVideo(submission.url, fileName)
        }

        // Add to list of downloads
        val hash = shell.calculateHash(fileName).orEmpty()
        downloads.add(Download(fileName, success, hash))

        println(if (success) " ✅ [Success]" else " ❌ [Error]")
    }

    return downloads
}

private fun cleanFailedDownloads(user: String, downloads: List<Download>) {
    println("\n🗑 Removing duplicated and failed downloads...")

    // Removing 0-byte files
    downloads.forEach {
        val file = File("/tmp/rmd/$user", it.fileName)
        if (file.exists() && file.length() == 0L) {
            println("ZER: ${file.absoluteFile}")
            file.delete()
        }
    }

    // Removing duplicates
    downloads.groupBy { it.hash }.values.forEach {
        it.drop(1).forEach { download ->
            val file = File("/tmp/rmd/$user", download.fileName)
            if (file.exists()) {
                println("DUP: ${file.absoluteFile}")
                file.delete()
            }
        }
    }
}