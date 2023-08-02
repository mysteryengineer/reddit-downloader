package io.vinicius.rmd

import com.github.ajalt.mordant.animation.Animation
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import io.vinicius.rmd.model.Download
import io.vinicius.rmd.model.Response
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.util.Fetch
import io.vinicius.rmd.util.Shell
import io.vinicius.rmd.util.Shell.Companion.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    val user = System.getenv("RMD_USER")
    val limit = System.getenv("RMD_LIMIT")?.toInt() ?: 1000
    val parallel = System.getenv("RMD_PARALLEL")?.toInt() ?: 5
    val convert = System.getenv("RMD_CONVERT_GIFS")?.toBoolean() ?: true

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

private fun getSubmissions(user: String, limit: Int): Set<Submission> {
    val fetch = Fetch()
    val submissions = mutableSetOf<Submission>()
    var after: String? = null
    var counter = 0

    print("\n📝 Collecting ${TextStyles.bold(limit.toString())} posts from user ${TextStyles.bold(user)} ")

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

    t.println(" ${min(submissions.size, limit)}/$limit unique posts found\n")

    return submissions.take(limit).toSet()
}

private fun createUrl(user: String, after: String): String {
    val baseUrl = "https://www.reddit.com/user/${user}/submitted.json"
    return "${baseUrl}?limit=100&sort=new&after=${after}&raw_json=1"
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun downloadMedia(user: String, submissions: Set<Submission>, parallel: Int): List<Download> {
    val downloads = mutableListOf<Download>()
    val shell = Shell(File("/tmp/rmd/$user"))
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    val padding = submissions.size.toString().count()
    val parallelismContext = Dispatchers.IO.limitedParallelism(parallel)

    val progress = t.progressAnimation {
        text("Downloading")
        percentage()
        progressBar()
        completed()
        speed(" posts/sec")
        timeRemaining()
    }

    progress.updateTotal(submissions.size.toLong())

    runBlocking {
        val jobs = submissions.mapIndexed { index, submission ->
            launch(parallelismContext) {
                val dateTime = LocalDateTime.ofEpochSecond(submission.data.created, 0, ZoneOffset.UTC)
                val number = (index + 1).toString().padStart(padding, '0')
                var fileName = "${formatter.format(dateTime)}-$user-$number"

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

                progress.update(downloads.size)
            }
        }

        progress.start()
        jobs.joinAll()
        progress.update(submissions.size.toLong())
        progress.stop()
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
    t.println("\n🚮 Removing duplicated downloads...")

    // Removing 0-byte files
    downloads.forEach {
        val file = File("/tmp/rmd/$user", it.fileName)
        if (file.exists() && file.length() == 0L) {
            t.println("[${TextColors.brightBlue("Z")}] ${file.absoluteFile}")
            file.delete()
        }
    }

    // Removing duplicates
    downloads.groupBy { it.hash }.values.forEach {
        it.drop(1).forEach { download ->
            val file = File("/tmp/rmd/$user", download.fileName)
            if (file.exists()) {
                t.println("[${TextColors.brightRed("D")}] ${file.absoluteFile}")
                file.delete()
            }
        }
    }
}

private fun convertGifs(user: String, downloads: List<Download>) {
    t.println("\n⚙️ Converting gifs to videos...")
    val shell = Shell(File("/tmp/rmd/$user"))

    val gifs = downloads
        .filter { it.isSuccess }
        .filter { it.fileName.endsWith(".gif") }

    gifs.forEach {
        val gifFile = File("/tmp/rmd/$user", it.fileName)
        val baseFile = gifFile.parent + File.separator + gifFile.nameWithoutExtension

        if (gifFile.exists()) {
            t.println("[${TextColors.green("C")}] Converting ${gifFile.absolutePath} to video...")
            shell.convertGif(baseFile)

            val mp4File = File("$baseFile.mp4")
            if (mp4File.exists()) gifFile.delete()
        }
    }
}

private fun createReport(user: String, downloads: List<Download>) {
    val totalFailed = downloads.count { !it.isSuccess }

    File("/tmp/rmd/$user", "_report.md").printWriter().use { out ->
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

private fun printMostRecent(submissions: List<Submission?>) {
    // TODO Implement download UI update
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