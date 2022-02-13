package com.amarland.svg2iv.outerworld

import androidx.compose.ui.graphics.vector.ImageVector
import com.amarland.svg2iv.util.RingBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import java.io.Reader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException

@Suppress("BlockingMethodInNonBlockingContext") // TODO: use ServerSocketChannel?
@Throws(
    IOException::class,
    InterruptedException::class,
    SecurityException::class
)
suspend fun callCliTool(
    sourceFilePaths: List<String>,
    startProcess: (
        sourceFilePaths: List<String>,
        extensionReceiver: String?,
        serverSocketAddress: InetAddress,
        serverSocketPort: Int,
    ) -> Process = ::startCliToolProcess,
    extensionReceiver: String? = null
): Pair<List<ImageVector?>, List<String>> {
    require(sourceFilePaths.isNotEmpty())

    return withContext(Dispatchers.IO) {
        val serverSocket = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).apply {
            soTimeout = 3000
        }
        val process = startProcess(
            sourceFilePaths,
            extensionReceiver,
            serverSocket.inetAddress,
            serverSocket.localPort
        )

        var imageVectors = emptyList<ImageVector?>()
        val errorMessages = process.errorStream
            .bufferedReader()
            .readLines(to = RingBuffer.create(50)) // uses `use` internally

        try {
            serverSocket.accept().use { client ->
                client.getInputStream().use { stream ->
                    imageVectors = ImageVectorArrayJsonAdapter()
                        .fromJson(stream.source().buffer())
                        ?: imageVectors
                }
            }
        } catch (e: SocketTimeoutException) {
            // do nothing?
        } finally {
            serverSocket.close()
        }

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
    extensionReceiver: String?,
    serverSocketAddress: InetAddress,
    serverSocketPort: Int,
): Process {
    val shellInvocation = if (IS_OS_WINDOWS) "cmd.exe" else "sh"
    val commandOption = if (IS_OS_WINDOWS) "/c" else "-c"
    val executableName = "svg2iv.exe"
    val executablePath =
        if (File(executableName).exists()) ".${File.separator}$executableName" else executableName
    val command = executablePath +
            (if (extensionReceiver.isNullOrEmpty()) "" else " -r $extensionReceiver") +
            " -s ${serverSocketAddress.hostAddress}:$serverSocketPort" +
            " \"" + sourceFilePaths.joinToString(",") + '"'
    return Runtime.getRuntime().exec(arrayOf(shellInvocation, commandOption, command))
}

private fun Reader.readLines(to: MutableList<String>): List<String> =
    to.apply { forEachLine(this::add) }
