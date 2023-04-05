package io.vinicius.rmd.util

import com.github.ajalt.mordant.terminal.ExperimentalTerminalApi
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

    fun findSimilar(type: String): List<List<String>> {
        val result = runCommand("czkawka $type -d ${directory.absolutePath}").getOrNull()

        return result?.split("\n\n")?.drop(1)?.map { group ->
            group.split("\n").drop(1)
                .map { data ->
                    data.split(" - ").first()
                }
        } ?: emptyList()
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
        @OptIn(ExperimentalTerminalApi::class)
        val t = Terminal()
    }
}