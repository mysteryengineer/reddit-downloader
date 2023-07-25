package io.vinicius.rmd.util

import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

@OptIn(ExperimentalForeignApi::class)
class Shell(private val directory: Path, private val debug: Boolean = false) {
    private val fs = FileSystem.SYSTEM

    init {
        if (!fs.exists(directory)) fs.createDirectories(directory)
    }

    fun downloadImage(url: String, output: String): Result<String> {
        val dest = directory.resolve(output)
        return runCommand("wget $url -O $dest -nv")
    }

    fun downloadVideo(url: String, output: String): Result<String> {
        val dest = directory.resolve(output)
        return runCommand("yt-dlp $url -o $dest")
    }

    fun calculateHash(fileName: String): String? {
        val file = directory.resolve(fileName)

        return if (fs.exists(file)) {
            runCommand("sha256sum $file").getOrNull()?.take(64)
        } else {
            null
        }
    }

    private fun runCommand(command: String): Result<String> {
        val fp = popen("$command 2>&1", "r") ?: throw Error("Failed to run command: $command")

        val stdout = buildString {
            val buffer = ByteArray(4096)
            while (true) {
                val input = fgets(buffer.refTo(0), buffer.size, fp)?.toKString()
                if (input.isNullOrEmpty()) break
                append(input)
            }
        }.trim()

        val status = pclose(fp)

        return if (status == 0) {
            Result.success(stdout)
        } else {
            Result.failure(Error(stdout))
        }
    }

    companion object {
        val t = Terminal()
    }
}