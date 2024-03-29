package com.amarland.svg2iv.outerworld

import net.harawata.appdirs.AppDirsFactory
import java.io.IOException
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

val APP_DATA_DIRECTORY_PATH_STRING: String =
    AppDirsFactory.getInstance().getUserDataDir("svg2iv-desktop", null, "com.amarland", true)

private val PROPERTIES_FILE_PATH = Path.of(APP_DATA_DIRECTORY_PATH_STRING, "svg2iv.properties")

private val LOG_FILE_PATH = Path.of(APP_DATA_DIRECTORY_PATH_STRING, "svg2iv.log")

fun writeProperties(properties: Properties) {
    tryOrIgnore {
        ensureParentDirectoriesExist(PROPERTIES_FILE_PATH)
        Files.newBufferedWriter(PROPERTIES_FILE_PATH).use { writer ->
            properties.store(writer, null)
        }
    }
}

fun readProperties() =
    Properties().apply {
        tryOrIgnore {
            if (Files.exists(PROPERTIES_FILE_PATH)) {
                Files.newBufferedReader(PROPERTIES_FILE_PATH).use(::load)
            }
        }
    }

fun writeErrorMessages(messageReader: Reader) {
    tryOrIgnore {
        ensureParentDirectoriesExist(LOG_FILE_PATH)
        Files.newBufferedWriter(LOG_FILE_PATH).use(messageReader::copyTo)
    }
}

fun readErrorMessages(limit: Int): Pair<List<String>, Boolean> {
    var messages: MutableList<String>? = null
    var hasMoreThanLimit = false
    tryOrIgnore {
        if (Files.exists(LOG_FILE_PATH)) {
            Files.newBufferedReader(LOG_FILE_PATH).useLines { lines ->
                messages = ArrayList(limit)
                lines.forEachIndexed { index, line ->
                    if (index == limit) {
                        hasMoreThanLimit = true
                        return@forEachIndexed
                    }
                    messages!!.add(line)
                }
            }
        }
    }
    return (messages ?: emptyList()) to hasMoreThanLimit
}

fun openLogFileInPreferredApplication() = openFile(LOG_FILE_PATH)

private fun ensureParentDirectoriesExist(path: Path) {
    Files.createDirectories(path.parent)
}

private inline fun <T> tryOrIgnore(block: () -> T): T? =
    try {
        block()
    } catch (e: IOException) {
        // well...
        null
    }
