package io.vinicius.rmd.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

@OptIn(ExperimentalForeignApi::class)
class Shell {
    fun calculateHash(): String {
        val result = runCommand("sha256sum /Users/vegidio/Desktop/test.txt")
        return result.getOrNull() ?: throw Error("Error calculating the hash")
    }

    fun runCommand(command: String): Result<String> {
        val fp = popen(command, "r") ?: throw Error("Failed to run command: $command")

        val stdout = buildString {
            val buffer = ByteArray(4096)
            while (true) {
                val input = fgets(buffer.refTo(0), buffer.size, fp)?.toKString()
                if (input.isNullOrEmpty()) break
                append(input)
            }
        }

        val status = pclose(fp)

        return if (status == 0) {
            Result.success(stdout)
        } else {
            Result.failure(Error("Error executing command: $command. Exit code: $status"))
        }
    }
}