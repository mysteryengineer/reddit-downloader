package io.vinicius.rmd

import io.vinicius.rmd.model.Download
import io.vinicius.rmd.model.Response
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.util.Fetch
import io.vinicius.rmd.util.Shell
import korlibs.time.DateFormat
import korlibs.time.DateTimeTz
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val user: String? = getenv("RMD_USER")?.toKString()
    val limit: Int? = getenv("RMD_LIMIT")?.toKString()?.toInt()
    val parallel: Int = getenv("RMD_PARALLEL")?.toKString()?.toInt() ?: 1

    if (user == null || limit == null) {
        error("🧨 Missing the environment variable RMD_USER or RMD_LIMIT")
    }

    val submissions = try {
        getSubmissions(user, limit)
    } catch (_: Exception) {
        error("\n🧨 Error fetching the posts; check if the servers are up by accessing the site " +
            "https://www.redditstatus.com")
    }

    val downloads = downloadMedia(user, submissions, parallel)
    println(downloads)
}

private fun getSubmissions(user: String, limit: Int): Set<Submission> {
    val fetch = Fetch()
    val submissions = mutableSetOf<Submission>()
    var after: String? = null
    var counter = 0

    print("\n📝 Collecting $limit posts from user $user ")

    do {
        val url = createUrl(user, after ?: "")
        val response = fetch.get<Response>(url).data
        val list = response.children
        submissions.addAll(list)

        // Getting the next "after"
        after = response.after
        counter++

        runBlocking {
            print(".")
            delay(2.seconds)
        }
    } while (list.isNotEmpty() && counter < ceil(limit / 100f) && after != null)

    println(" ${min(submissions.size, limit)}/$limit unique posts found\n")

    return submissions.take(limit).toSet()
}

private fun createUrl(user: String, after: String): String {
    val baseUrl = "https://www.reddit.com/user/${user}/submitted.json"
    return "${baseUrl}?limit=100&sort=new&after=${after}&raw_json=1"
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun downloadMedia(user: String, submissions: Set<Submission>, parallel: Int): List<Download> {

    val downloads = mutableListOf<Download>()
    val shell = Shell("/tmp/rmd/$user".toPath())
    val formatter = DateFormat("yyyyMMdd-HHmmss")
    val padding = submissions.size.toString().count()
    val parallelismContext = Dispatchers.IO.limitedParallelism(parallel)

    runBlocking {
        val jobs = submissions.mapIndexed { index, submission ->
            launch(parallelismContext) {
                val dateTime = DateTimeTz.fromUnixLocal(submission.data.created * 1000)
                val number = (index + 1).toString().padStart(padding, '0')
                var fileName = "${formatter.format(dateTime)}-$user-$number"

                printStatus(index, padding, submission)

                val result = if (getMediaType(submission) == MediaType.Image) {
                    val extension = getFileExtension(submission.data.url) ?: "jpg"
                    fileName += ".$extension"
                    shell.downloadImage(submission.data.url, fileName)
                } else {
                    fileName += ".mp4"
                    shell.downloadVideo(submission.data.url, fileName)
                }

                // Add to list of downloads
                val hash = shell.calculateHash(fileName).orEmpty()

                downloads.add(
                    Download(
                        url = submission.data.url,
                        fileName = fileName,
                        output = result.getOrNull() ?: result.exceptionOrNull()?.message ?: "<Nothing>",
                        isSuccess = result.isSuccess,
                        hash = hash
                    )
                )
            }
        }

        jobs.joinAll()
    }

    return downloads
}

fun getFileExtension(fileName: String): String? {
    val lastDotIndex = fileName.lastIndexOf('.')
    return if (lastDotIndex >= 0 && lastDotIndex < fileName.length - 1) {
        fileName.substring(lastDotIndex + 1)
    } else {
        null
    }
}

private fun getMediaType(submission: Submission): MediaType {
    return if (submission.data.url.endsWith(".jpg") || submission.data.url.endsWith(".jpeg")) {
        MediaType.Image
    } else if (submission.data.url.endsWith(".png")) {
        MediaType.Image
    } else if (submission.data.url.endsWith(".gifv") || submission.data.url.endsWith(".mp4")) {
        MediaType.Video
    } else if (submission.data.postHint == "image") {
        MediaType.Image
    } else {
        MediaType.Video
    }
}

private fun printStatus(index: Int, padding: Int, submission: Submission) {
    val emoji: String
    val label: String
    val number = "${index + 1}".padStart(padding, '0')
    val url = submission.data.url.take(68)

    if (submission.data.postHint == "image") {
        emoji = "📸"
        label = "image"
    } else {
        emoji = "📹"
        label = "video"
    }

    println("$emoji [$number] Downloading $label $url ".padEnd(100, '.'))
}