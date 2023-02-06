package com.amarland.svg2iv.outerworld

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

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
