package io.vinicius.rmd.util

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class Shell(private val directory: File, private val debug: Boolean = false) {
    init {
        if (!directory.exists()) directory.mkdirs()
    }

    fun downloadImage(url: String, output: String): Boolean {
        val dest = File(directory, output)
        return runCommand("wget $url -O ${dest.absoluteFile} -nv").isSuccess
    }

    fun downloadVideo(url: String, output: String): Boolean {
        val dest = File(directory, output)
        return runCommand("youtube-dl $url -o ${dest.absoluteFile}").isSuccess
    }

    fun calculateHash(fileName: String): String? {
        val file = File(directory, fileName)

        return if (file.exists()) {
            runCommand("sha256sum ${file.absoluteFile}").getOrNull()?.take(64)
        } else {
            null
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
        val output = processOutput(proc)

        if (proc.exitValue() == 0) {
            Result.success(output)
        } else {
            Result.failure(IOException(output))
        }
    } catch (e: IOException) {
        Result.failure(e)
    }

    private fun processOutput(proc: Process): String {
        val output = if (proc.exitValue() == 0) {
            proc.inputReader().readText()
        } else {
            proc.errorReader().readText()
        }

        if (debug) println(output)
        return output
    }
    // endregion
}