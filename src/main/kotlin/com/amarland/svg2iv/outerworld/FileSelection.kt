package com.amarland.svg2iv.outerworld

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.util.*
import javax.swing.JFileChooser

suspend fun openFileSelectionDialog(parent: Frame): List<String> {
    when {
        IS_OS_WINDOWS -> {
            val files = readPowerShellScriptOutputLines(
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

            if (files != null) return files
        }

        IS_OS_MACOS ->
            readShellCommandOutputLines(
                """osascript -e "choose file""" +
                        """ of type { "*.svg", "*.xml" } with multiple selections allowed""""
            )?.let { lines ->
                if (lines.isEmpty()) return lines

                val files = lines.singleOrNull()
                    ?.split(", ")
                    ?.takeUnless { paths -> paths.isEmpty() }
                    ?.map { path ->
                        path.split(':')
                            .drop(1) // "alias Macintosh HD"
                            .joinToString("\\")
                    }

                if (files != null) return files
            }

        else ->
            // TODO: check whether qarma is available first
            readShellCommandOutputLines(
                "zenity --file-selection --file-filter=\"*.svg *.xml\"" +
                        " --multiple --separator :"
            )?.let { lines ->
                if (lines.isEmpty()) return lines

                val files = lines.singleOrNull()
                    ?.split(":")
                    ?.takeUnless { it.isEmpty() }

                if (files != null) return files
            }
    }

    return FileDialog(parent).apply {
        setFilenameFilter { _, name -> name.endsWith(".svg") || name.endsWith(".xml") }
        isMultipleMode = true
        isVisible = true
    }.files.map { file -> file.absolutePath }
}

suspend fun openDirectorySelectionDialog(parent: Frame): String? {
    when {
        IS_OS_WINDOWS ->
            readPowerShellScriptOutputLines(
                """
                Add-Type -AssemblyName System.Windows.Forms
                ${'$'}dialog = New-Object System.Windows.Forms.FolderBrowserDialog
                if (${'$'}dialog.ShowDialog() -eq [Windows.Forms.DialogResult]::OK) {
                    Write-Output ${'$'}dialog.SelectedPath
                }""".trimIndent()
            )?.let { lines ->
                if (lines.isEmpty()) return null

                val path = lines.singleOrNull()?.takeUnless { path -> path.isEmpty() }
                if (path != null) return path
            }

        IS_OS_MACOS ->
            readShellCommandOutputLines("""osascript -e "choose directory"""")
                ?.let { lines ->
                    if (lines.isEmpty()) return null

                    val path = lines.singleOrNull()
                        ?.split(":")
                        ?.drop(1) // "alias Macintosh HD"
                        ?.joinToString("\\")
                        ?.takeUnless { path -> path.isEmpty() }

                    if (path != null) return path
                }

        else ->
            readShellCommandOutputLines("zenity --file-selection --directory")
                ?.let { lines ->
                    if (lines.isEmpty()) return null

                    val path = lines.singleOrNull()?.takeUnless { it.isEmpty() }
                    if (path != null) return path
                }
    }

    return JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        showDialog(parent, "Select")
    }.selectedFile.absolutePath
}

private suspend fun readPowerShellScriptOutputLines(script: String): List<String>? =
    withContext(Dispatchers.IO) {
        val encodedScript = Base64.getEncoder()
            .encode(script.toByteArray(Charsets.UTF_16LE /* System.Text.Encoding.Unicode */))
            .decodeToString()
        return@withContext Runtime.getRuntime()
            .exec("powershell -ExecutionPolicy unrestricted -EncodedCommand $encodedScript")
            .readOutputLines()
    }

private suspend fun readShellCommandOutputLines(command: String): List<String>? =
    withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec(arrayOf("sh", "-c", command)).readOutputLines()
    }

/* file selected                      -> non-empty list
 * file not selected but dialog shown -> empty list
 * dialog not shown                   -> null
 */
private suspend fun Process.readOutputLines(): List<String>? =
    onExit().await()
        .takeIf { exitValue() == 0 }
        ?.inputStream?.bufferedReader()
        ?.readLines()
