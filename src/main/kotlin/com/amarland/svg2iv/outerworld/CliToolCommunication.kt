package com.amarland.svg2iv.outerworld

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.IOException
import java.io.Reader

@Suppress("BlockingMethodInNonBlockingContext")
@Throws(
    IOException::class,
    InterruptedException::class,
    SecurityException::class
)
suspend fun callCliTool(
    sourceFilePaths: List<String>,
    extensionReceiver: String? = null,
    startProcess: (
        sourceFilePaths: List<String>,
        extensionReceiver: String?
    ) -> Process = ::startCliToolProcess,
    doWithErrorMessages: (messageReader: Reader) -> Unit = ::writeErrorMessages
): List<ImageVector?> {
    require(sourceFilePaths.isNotEmpty())

    return withContext(Dispatchers.IO) {
        val process = startProcess(sourceFilePaths, extensionReceiver)
        val imageVectors = process.inputStream.source().buffer()
            .use { bufferedSource ->
                bufferedSource.takeUnless { source -> source.exhausted() }
                    ?.let { source -> ImageVectorArrayJsonAdapter().fromJson(source) }
                    ?: emptyList()
            }
        process.errorStream.bufferedReader().use(doWithErrorMessages)
        process.waitFor()

        return@withContext imageVectors
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
    val shellInvocation = if (IS_OS_WINDOWS) "powershell.exe" else "sh"
    val commandOption = if (IS_OS_WINDOWS) "-Command" else "-c"
    val executablePath = "./bin/svg2iv.exe"
    val command = buildString {
        append(executablePath)
        if (!extensionReceiver.isNullOrEmpty()) {
            append(" -r $extensionReceiver")
        }
        append(" --json ")
        append('"')
        append(sourceFilePaths.joinToString(","))
        append('"')
    }
    return Runtime.getRuntime().exec(arrayOf(shellInvocation, commandOption, command))
}
