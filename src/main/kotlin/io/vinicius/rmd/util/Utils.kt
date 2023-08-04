package io.vinicius.rmd.util

import com.github.ajalt.mordant.animation.Animation
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import io.vinicius.rmd.helper.Shell.Companion.t
import io.vinicius.rmd.model.Submission
import io.vinicius.rmd.type.MediaType

fun createUrl(user: String, after: String): String {
    val baseUrl = "https://www.reddit.com/user/${user}/submitted.json"
    return "${baseUrl}?limit=100&sort=new&after=${after}&raw_json=1"
}

fun getFileExtension(fileName: String): String? {
    val lastDotIndex = fileName.lastIndexOf('.')
    return if (lastDotIndex >= 0 && lastDotIndex < fileName.length - 1) {
        fileName.substring(lastDotIndex + 1)
    } else {
        null
    }
}

fun getMediaType(submission: Submission): MediaType {
    return if (submission.data.url.endsWith(".jpg") || submission.data.url.endsWith(".jpeg")) {
        MediaType.Image
    } else if (submission.data.url.endsWith(".png")) {
        MediaType.Image
    } else if (submission.data.url.endsWith(".gifv") || submission.data.url.endsWith(".mp4")) {
        MediaType.Video
    } else if (submission.data.url.endsWith(".gif")) {
        MediaType.Image
    } else if (submission.data.postHint == "image") {
        MediaType.Image
    } else {
        MediaType.Video
    }
}

fun printMostRecent(padding: Int): Animation<List<Pair<Int, Submission>>> {
    return t.textAnimation { list ->
        list.joinToString("\n", "\n") { (index, submission) ->
            downloadInfo(index, padding, submission)
        }
    }
}

private fun downloadInfo(index: Int, padding: Int, submission: Submission): String {
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

    return "$emoji [$number] Downloading $label $url ..."
}