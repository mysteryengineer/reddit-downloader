package io.vinicius.rmd.helper

import com.github.ajalt.mordant.terminal.Terminal
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class Shell(private val directory: File, private val debug: Boolean = false) {
    init {
        if (!directory.exists()) directory.mkdirs()
    }

    fun downloadImage(url: String, output: String): Result<String> {
        val dest = File(directory, output)
        return runCommand("wget $url -O ${dest.absoluteFile} -nv")
    }

    fun downloadVideo(url: String, output: String): Result<String> {
        val dest = File(directory, output)
        return runCommand("yt-dlp $url -o ${dest.absoluteFile}")
    }

    fun calculateHash(fileName: String): String? {
        val file = File(directory, fileName)

        return if (file.exists()) {
            runCommand("sha256sum ${file.absoluteFile}").getOrNull()?.take(64)
        } else {
            null
        }
    }

    fun convertToWebp(oldFile: String, newFile: String) {
        runCommand("cwebp $oldFile -o $newFile")
    }

    fun convertToWebm(oldFile: String, newFile: String) {
        if (oldFile.endsWith(".gif", true)) {
            runCommand("ffmpeg -i $oldFile -row-mt 1 -movflags faststart -pix_fmt yuv420p " +
                "-vf scale=trunc(iw/2)*2:trunc(ih/2)*2 $newFile")
        } else {
            runCommand("ffmpeg -i $oldFile -row-mt 1 $newFile")
        }
    }

    // region - Private methods
    private fun runCommand(
        command: String,
        workingDir: File = File("."),
        timeout: Duration = 60.minutes
    ): Result<String> = try {
        if (debug) println("\n\$ $command")

        val parts = command.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        processResult(proc)
    } catch (e: IOException) {
        Result.failure(e)
    }

    private fun processResult(proc: Process): Result<String> {
        val output: String
        val result = if (proc.exitValue() == 0) {
            output = proc.inputReader().readText().trim()
            Result.success(output)
        } else {
            output = proc.errorReader().readText().trim()
            Result.failure(IOException(output))
        }

        if (debug) println(output)
        return result
    }
    // endregion

    companion object {
        val t = Terminal()
    }
}