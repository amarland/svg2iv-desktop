package com.amarland.svg2iv.outerworld

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Base64
import javax.swing.JFileChooser

fun openFileChooser(
    parent: Frame,
    mode: FileSystemEntitySelectionMode
): Array<File> {
    return when (mode) {
        FileSystemEntitySelectionMode.SOURCE -> {
            if (isOSWindows()) {
                val script = """
                    Add-Type -AssemblyName System.Windows.Forms
                    ${'$'}dialog = New-Object System.Windows.Forms.OpenFileDialog -Property @{
                        Filter = 'SVG files (*.svg)|*.svg|XML files (*.xml)|*.xml|All files (*.*)|*.*'
                        Multiselect = ${'$'}true
                    }
                    if (${'$'}dialog.ShowDialog() -eq [Windows.Forms.DialogResult]::OK) {
                        Write-Output ${'$'}dialog.FileNames
                    }""".trimIndent()
                val encodedScript = Base64.getEncoder()
                    .encode(script.toByteArray(Charsets.UTF_16LE))
                    .decodeToString()
                with(
                    Runtime.getRuntime().exec(
                        "powershell -ExecutionPolicy unrestricted -EncodedCommand $encodedScript"
                    )
                ) {
                    waitFor()
                    if (exitValue() == 0) {
                        return inputStream
                            .bufferedReader()
                            .readLines()
                            .map { path -> File(path) }
                            .toTypedArray()
                    }
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

enum class FileSystemEntitySelectionMode { SOURCE, DESTINATION }
