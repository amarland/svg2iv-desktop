package com.amarland.svg2iv.outerworld

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Base64
import javax.swing.JFileChooser

suspend fun openFileChooser(
    parent: Frame,
    mode: FileSystemEntitySelectionMode
): Array<File> {
    return when (mode) {
        FileSystemEntitySelectionMode.SOURCE -> {
            if (isOSWindows()) {
                val lines = executeScript(
                    """
                    Add-Type -AssemblyName System.Windows.Forms
                    ${'$'}dialog = New-Object System.Windows.Forms.OpenFileDialog -Property @{
                        Filter = 'SVG files (*.svg)|*.svg|XML files (*.xml)|*.xml|All files (*.*)|*.*'
                        Multiselect = ${'$'}true
                    }
                    if (${'$'}dialog.ShowDialog() -eq [Windows.Forms.DialogResult]::OK) {
                        Write-Output ${'$'}dialog.FileNames
                    }
                    """.trimIndent()
                )
                if (lines.isNotEmpty()) {
                    return lines.map { path -> File(path) }.toTypedArray()
                }
            }

            return FileDialog(parent).apply {
                file = "*.svg" // for Windows
                setFilenameFilter { _, name -> name.endsWith(".svg") } // for other OSes
                isMultipleMode = true
                isVisible = true
            }.files ?: emptyArray()
        }

        FileSystemEntitySelectionMode.DESTINATION -> {
            with(JFileChooser()) {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                showDialog(parent, "Select")
                return@with selectedFiles?.takeIf { it.isNotEmpty() }
                    ?: selectedFile?.let { arrayOf(it) } ?: emptyArray()
            }
        }
    }
}

private suspend fun executeScript(script: String): List<String> {
    val encodedScript = Base64.getEncoder()
        .encode(script.toByteArray(Charsets.UTF_16LE))
        .decodeToString()
    return withContext(Dispatchers.IO) {
        with(
            Runtime.getRuntime()
                .exec("powershell -ExecutionPolicy unrestricted -EncodedCommand $encodedScript")
        ) {
            if (onExit().await().exitValue() == 0) {
                return@with inputStream.bufferedReader().readLines()
            }
            return@with emptyList()
        }
    }
}

enum class FileSystemEntitySelectionMode { SOURCE, DESTINATION }
