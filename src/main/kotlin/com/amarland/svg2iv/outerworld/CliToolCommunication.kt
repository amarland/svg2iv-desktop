package com.amarland.svg2iv.outerworld

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
@Throws(
    IOException::class,
    InterruptedException::class,
    SecurityException::class
)
suspend fun callCliTool(
    sourceFilePaths: List<String>,
    startProcess: (
        sourceFilePaths: List<String>,
        extensionReceiver: String?
    ) -> Process = ::startCliToolProcess,
    extensionReceiver: String? = null
): Pair<List<ImageVector?>, List<String>> {
    require(sourceFilePaths.isNotEmpty())

    return withContext(Dispatchers.IO) {
        val process = startProcess(sourceFilePaths, extensionReceiver)

        val imageVectors = ImageVectorArrayJsonAdapter()
            .fromJson(process.inputStream.source().buffer())
            ?: emptyList<ImageVector>()
        val errorMessages = process.errorStream
            .bufferedReader()
            .readLines() // uses `use` internally

        process.waitFor()

        return@withContext imageVectors to errorMessages
    }
}

@Throws(
    IOException::class,
    SecurityException::class
)
private fun startCliToolProcess(
    sourceFilePaths: List<String>,
    extensionReceiver: String?
): Process {
    val shellInvocation = if (IS_OS_WINDOWS) "cmd.exe" else "sh"
    val commandOption = if (IS_OS_WINDOWS) "/c" else "-c"
    val executableName = "svg2iv.exe"
    val executablePath =
        if (File(executableName).exists()) ".${File.separator}$executableName" else executableName
    val command = buildString {
        append(executablePath)
        if (!extensionReceiver.isNullOrEmpty()) {
            append(" -r $extensionReceiver")
        }
        append(" --stdout ")
        append('"')
        append(sourceFilePaths.joinToString(","))
        append('"')
    }
    return Runtime.getRuntime().exec(arrayOf(shellInvocation, commandOption, command))
}
