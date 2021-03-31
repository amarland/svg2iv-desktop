package com.amarland.svg2iv.outerworld

import kotlinx.coroutines.future.await
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Base64
import javax.swing.JFileChooser

suspend fun openFileSelectionDialog(parent: Frame): List<File> {
    when {
        IS_OS_WINDOWS ->
            readPowerShellScriptOutputLines(
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
            )?.let { lines ->
                return if (lines.isEmpty()) emptyList() else lines.map { path -> File(path) }
            }

        IS_OS_MACOS ->
            readShellCommandOutputLines(
                """osascript -e "choose file""" +
                        """ of type { "*.svg", "*.xml" } with multiple selections allowed""""
            )?.let { lines ->
                if (lines.isEmpty()) return emptyList()

                val files = lines.singleOrNull()
                    ?.split(", ")
                    ?.takeUnless { path -> path.isEmpty() }
                    ?.map { path ->
                        path.split(File.pathSeparatorChar)
                            .drop(1) // "alias Macintosh HD"
                            .joinToString(File.separator)
                    }?.map(::File)

                if (files != null) return files
            }

        else ->
            readShellCommandOutputLines(
                """zenity --file-selection --file-filter="*.svg *.xml" --multiple --separator """ +
                        File.pathSeparatorChar
            )?.let { lines ->
                if (lines.isEmpty()) return emptyList()

                val files = lines.singleOrNull()
                    ?.split(":")
                    ?.takeUnless { it.isEmpty() }
                    ?.map(::File)

                if (files != null) return files
            }
    }

    return FileDialog(parent).apply {
        setFilenameFilter { _, name -> name.endsWith(".svg") || name.endsWith(".xml") }
        isMultipleMode = true
        isVisible = true
    }.files.toList()
}

suspend fun openDirectorySelectionDialog(parent: Frame): File? {
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
                if (path != null) return File(path)
            }

        IS_OS_MACOS ->
            readShellCommandOutputLines("""osascript -e "choose directory"""")
                ?.let { lines ->
                    if (lines.isEmpty()) return null

                    val path = lines.singleOrNull()
                        ?.split(":")
                        ?.drop(1) // "alias Macintosh HD"
                        ?.joinToString(File.separator)
                        ?.takeUnless { path -> path.isEmpty() }

                    if (path != null) return File(path)
                }

        else ->
            readShellCommandOutputLines("zenity --file-selection --directory")
                ?.let { lines ->
                    if (lines.isEmpty()) return null

                    val path = lines.singleOrNull()?.takeUnless { it.isEmpty() }
                    if (path != null) return File(path)
                }
    }

    return JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        showDialog(parent, "Select")
    }.selectedFile
}

private suspend fun readPowerShellScriptOutputLines(script: String): List<String>? {
    val encodedScript = Base64.getEncoder()
        .encode(script.toByteArray(Charsets.UTF_16LE /* System.Text.Encoding.Unicode */))
        .decodeToString()
    return Runtime.getRuntime()
        .exec("powershell -ExecutionPolicy unrestricted -EncodedCommand $encodedScript")
        .readOutputLines()
}

private suspend fun readShellCommandOutputLines(command: String) =
    Runtime.getRuntime().exec(arrayOf("sh", "-c", command)).readOutputLines()

/* file selected                      -> non-empty list
 * file not selected but dialog shown -> empty list
 * dialog not shown                   -> null
 */
private suspend fun Process.readOutputLines(): List<String>? =
    takeIf { onExit().await().exitValue() == 0 }
        ?.inputStream
        ?.bufferedReader()
        ?.readLines()
