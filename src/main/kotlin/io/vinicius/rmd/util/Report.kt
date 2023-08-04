package io.vinicius.rmd.util

import io.vinicius.rmd.model.Download
import java.io.File

fun createReport(user: String, downloads: List<Download>) {
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