package io.vinicius.rmd

import com.github.ajalt.mordant.animation.Animation
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import io.vinicius.rmd.model.Download
import io.vinicius.rmd.model.Response
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.util.Fetch
import io.vinicius.rmd.util.Shell
import io.vinicius.rmd.util.Shell.Companion.t
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
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val user: String? = getenv("RMD_USER")?.toKString()
    val limit: Int = getenv("RMD_LIMIT")?.toKString()?.toInt() ?: 1000
    val parallel: Int = getenv("RMD_PARALLEL")?.toKString()?.toInt() ?: 5

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

    t.println("\n🌟 Done!")
}

private fun getSubmissions(user: String, limit: Int): Set<Submission> {
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

        // Getting the next "after"
        after = response.after
        counter++

        runBlocking {
            t.print(".")
            delay(2.seconds)
        }
    } while (list.isNotEmpty() && counter < ceil(limit / 100f) && after != null)

    t.println(" ${min(submissions.size, limit)}/$limit unique posts found\n")
    t.println()

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

                val anim = printAnimation(index, padding, submission)
                anim.update(DownloadStatus.Downloading)

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

                // Updating the animation
                if (result.isSuccess) {
                    anim.update(DownloadStatus.Success)
                } else {
                    anim.update(DownloadStatus.Failure)
                }
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

private fun removeDuplicates(user: String, downloads: List<Download>) {
    val fs = FileSystem.SYSTEM
    t.println("\n🚮 Removing duplicated downloads...")

    // Removing duplicates
    downloads.groupBy { it.hash }.values.forEach {
        it.drop(1).forEach { download ->
            val file = "/tmp/rmd/$user".toPath().resolve(download.fileName)
            if (fs.exists(file)) {
                t.println("[${TextColors.brightRed("D")}] $file")
                fs.delete(file)
            }
        }
    }
}

private fun createReport(user: String, downloads: List<Download>) {
    val fs = FileSystem.SYSTEM
    val totalFailed = downloads.count { !it.isSuccess }
    val report = "/tmp/rmd/$user".toPath().resolve("_report.md")

    fs.write(report) {
        writeUtf8("# RMD - Download Report\n")
        writeUtf8("## Failed Downloads\n")
        writeUtf8("- Total: $totalFailed\n")

        downloads
            .filter { !it.isSuccess }
            .forEach {
                writeUtf8("### 🔗 Link: ${it.url} - ❌ **Failure**\n")
                writeUtf8("### 📝 Output:\n")
                writeUtf8("```\n")
                writeUtf8("${it.output}\n")
                writeUtf8("```\n")

                writeUtf8("---\n")
            }
    }
}

private fun printAnimation(index: Int, padding: Int, submission: Submission): Animation<DownloadStatus> {
    val emoji: String
    val label: String
    val number = TextStyles.bold(TextColors.blue((index + 1).toString().padStart(padding, '0')))
    val url = TextStyles.underline(TextColors.brightCyan(submission.data.url.take(68)))

    if (submission.data.postHint == "image") {
        emoji = "📸"
        label = TextColors.magenta("image")
    } else {
        emoji = "📹"
        label = TextColors.yellow("video")
    }

    val base = "$emoji [$number] Downloading $label $url ".padEnd(141, '.')

    return t.textAnimation {
        when (it) {
            DownloadStatus.Downloading -> base
            DownloadStatus.Success -> base + " ✅ ${TextStyles.bold(TextColors.green("Success"))}"
            DownloadStatus.Failure -> base + " ❌ ${TextStyles.bold(TextColors.red("Failure"))}"
        }
    }
}