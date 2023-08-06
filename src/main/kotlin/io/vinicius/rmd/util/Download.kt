package io.vinicius.rmd.util

import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors
import io.vinicius.rmd.helper.Shell
import io.vinicius.rmd.helper.Shell.Companion.t
import io.vinicius.rmd.model.Download
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.type.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
fun downloadMedia(user: String, submissions: Set<Submission>, parallel: Int): List<Download> {
    val downloads = mutableListOf<Download>()
    val shell = Shell(File("/tmp/rmd/$user"))
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    val padding = submissions.size.toString().count()
    val parallelismContext = Dispatchers.IO.limitedParallelism(parallel)
    val current = mutableListOf<Pair<Int, Submission>>()
    val anim = printMostRecent(padding)

    val progress = t.progressAnimation {
        text("Downloading")
        percentage()
        progressBar(width = 50)
        completed()
        speed(" posts/sec")
        timeRemaining()
    }

    progress.updateTotal(submissions.size.toLong())

    runBlocking {
        val jobs = submissions.mapIndexed { index, submission ->
            launch(parallelismContext) {
                current.add(Pair(index, submission))
                anim.update(current.takeLast(5))

                val dateTime = LocalDateTime.ofEpochSecond(submission.data.created, 0, ZoneOffset.UTC)
                val number = (index + 1).toString().padStart(padding, '0')
                var fileName = "${formatter.format(dateTime)}-$user-$number"

                val result = if (submission.data.type == MediaType.Image) {
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

        progress.stop()
        anim.stop()
    }

    return downloads
}

fun removeDuplicates(user: String, downloads: List<Download>) {
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

@OptIn(ExperimentalCoroutinesApi::class)
fun convertImages(user: String, downloads: List<Download>) {
    val shell = Shell(File("/tmp/rmd/$user"))
    val parallelismContext = Dispatchers.IO.limitedParallelism(5)

    val images = downloads
        .filter { it.isSuccess }
        .filter { it.fileName.endsWith(".jpg") || it.fileName.endsWith(".jpeg") || it.fileName.endsWith(".png")}

    if (images.isNotEmpty()) t.println("\n⚙️ Converting images to WebP...")

    runBlocking {
        val jobs = images.map {
            launch(parallelismContext) {
                val oldFile = File("/tmp/rmd/$user", it.fileName)
                val oldExtension = ".${oldFile.extension}"
                val newFile = File("/tmp/rmd/$user", it.fileName.replace(oldExtension, ".webp"))

                if (oldFile.exists()) {
                    t.println("[${TextColors.green("C")}] Converting ${oldFile.absolutePath} to WebP...")
                    shell.convertToWebp(oldFile.absolutePath, newFile.absolutePath)

                    // If the file was converted successfully, then we delete the original file
                    if (newFile.exists()) oldFile.delete()
                }
            }
        }

        jobs.joinAll()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun convertVideos(user: String, downloads: List<Download>) {
    val shell = Shell(File("/tmp/rmd/$user"))
    val parallelismContext = Dispatchers.IO.limitedParallelism(5)

    val videos = downloads
        .filter { it.isSuccess }
        .filter { it.fileName.endsWith(".gif") || it.fileName.endsWith(".mp4") }

    if (videos.isNotEmpty()) t.println("\n⚙️ Converting videos to WebM...")

    runBlocking {
        val jobs = videos.map {
            launch(parallelismContext) {
                val oldFile = File("/tmp/rmd/$user", it.fileName)
                val oldExtension = ".${oldFile.extension}"
                val newFile = File("/tmp/rmd/$user", it.fileName.replace(oldExtension, ".webm"))

                if (oldFile.exists()) {
                    t.println("[${TextColors.green("C")}] Converting ${oldFile.absolutePath} to WebM...")
                    shell.convertToWebm(oldFile.absolutePath, newFile.absolutePath)

                    // If the file was converted successfully, then we delete the original file
                    if (newFile.exists()) oldFile.delete()
                }
            }
        }

        jobs.joinAll()
    }
}