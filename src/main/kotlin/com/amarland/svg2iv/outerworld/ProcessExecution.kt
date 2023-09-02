package com.amarland.svg2iv.outerworld

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.nio.file.Path

suspend fun exec(vararg command: String): List<String>? =
    withContext(Dispatchers.IO) {
        runCatching {
            Runtime.getRuntime().exec(command)
                .onExit().await()
                .takeIf { process -> process.exitValue() == 0 }
                ?.inputStream?.bufferedReader()
                ?.readLines()
        }.getOrNull()
    }

fun openFile(filePath: Path) {
    open(
        filePath.toString().let { path ->
            if (IS_OS_WINDOWS) "\"$path\""
            else path.replace(" ", "\\ ")
        }
    )
}

fun openUrl(url: String) = open(if (IS_OS_WINDOWS) "\"\" $url" else url)

private fun open(filePathOrUrl: String) {
    Runtime.getRuntime().exec(
        when {
            IS_OS_WINDOWS -> "cmd /c start $filePathOrUrl"
            IS_OS_MACOS -> "open $filePathOrUrl"
            else -> "xdg-open $filePathOrUrl"
        }
    )
}
