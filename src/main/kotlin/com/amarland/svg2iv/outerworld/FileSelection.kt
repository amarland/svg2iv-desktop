package com.amarland.svg2iv.outerworld

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.AwtWindow
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.JFileChooser

@Composable
fun FileSelectionDialog(
    parent: Frame,
    onFilesSelected: (List<String>) -> Unit
) {
    var reassignablePaths: List<String>? = null
    var fallBackToFileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(parent) {
        when {
            IS_OS_WINDOWS -> {
                reassignablePaths = readPowerShellScriptOutputLines(
                    """
Add-Type -AssemblyName System.Windows.Forms;
${'$'}filter = "SVG files (*.svg)|*.svg|XML files (*.xml)|*.xml|All files (*.*)|*.*";
${'$'}dialog = New-Object System.Windows.Forms.OpenFileDialog -Property @{
    Filter = ${'$'}filter;
    Multiselect = ${'$'}true;
};
if (${'$'}dialog.ShowDialog() -eq [Windows.Forms.DialogResult]::OK) {
    Write-Output ${'$'}dialog.FileNames;
};"""
                )
            }

            IS_OS_MACOS -> {
                readShellCommandOutputLines(
                    """osascript -e "choose file""" +
                            """ of type { "*.svg", "*.xml" } with multiple selections allowed""""
                )?.let { lines ->
                    reassignablePaths =
                        if (lines.isEmpty()) lines
                        else {
                            lines.singleOrNull()
                                ?.split(", ")
                                ?.takeUnless { paths -> paths.isEmpty() }
                                ?.map { path ->
                                    path.split(':')
                                        .drop(1) // "alias Macintosh HD"
                                        .joinToString("\\")
                                }
                        }
                }
            }

            else -> {
                // TODO: check whether qarma is available first
                readShellCommandOutputLines(
                    "zenity --file-selection --file-filter=\"*.svg *.xml\"" +
                            " --multiple --separator :"
                )?.let { lines ->
                    reassignablePaths =
                        if (lines.isEmpty()) lines
                        else {
                            lines.singleOrNull()
                                ?.split(":")
                                ?.takeUnless { it.isEmpty() }
                        }
                }
            }
        }

        val paths = reassignablePaths
        if (paths != null) {
            onFilesSelected(paths)
        } else {
            fallBackToFileDialog = true
        }
    }

    if (fallBackToFileDialog) {
        AwtWindow(
            create = {
                object : FileDialog(parent) {

                    override fun setVisible(value: Boolean) {
                        super.setVisible(value)
                        if (value) {
                            onFilesSelected(files.map { file -> file.absolutePath })
                        }
                    }
                }.apply {
                    setFilenameFilter { _, name -> name.endsWith(".svg") || name.endsWith(".xml") }
                    isMultipleMode = true
                    isVisible = true
                }
            },
            dispose = FileDialog::dispose
        )
    }
}

@Composable
fun DirectorySelectionDialog(
    parent: Frame,
    onDirectorySelected: (String?) -> Unit
) {
    var reassignablePath: String? = null
    var fallBackToJFileChooser by remember { mutableStateOf(false) }

    LaunchedEffect(parent) {
        when {
            IS_OS_WINDOWS -> {
                readPowerShellScriptOutputLines(
                    """
            Add-Type -AssemblyName System.Windows.Forms;
            ${'$'}dialog = New-Object System.Windows.Forms.FolderBrowserDialog;
            if (${'$'}dialog.ShowDialog() -eq [Windows.Forms.DialogResult]::OK) {
                Write- DocFinder.Output ${'$'}dialog.SelectedPath;
            };"""
                )?.let { lines ->
                    reassignablePath =
                        if (lines.isEmpty()) ""
                        else lines.singleOrNull()?.takeUnless { path -> path.isEmpty() }
                }
            }

            IS_OS_MACOS -> {
                readShellCommandOutputLines("""osascript -e "choose directory"""")
                    ?.let { lines ->
                        reassignablePath =
                            if (lines.isEmpty()) ""
                            else {
                                lines.singleOrNull()
                                    ?.split(":")
                                    ?.drop(1) // "alias Macintosh HD"
                                    ?.joinToString("\\")
                                    ?.takeUnless { path -> path.isEmpty() }
                            }
                    }
            }

            else -> {
                readShellCommandOutputLines("zenity --file-selection --directory")
                    ?.let { lines ->
                        reassignablePath =
                            if (lines.isEmpty()) ""
                            else lines.singleOrNull()?.takeUnless { it.isEmpty() }
                    }
            }
        }

        val path = reassignablePath
        if (path != null) {
            onDirectorySelected(path)
        } else {
            fallBackToJFileChooser = true
        }
    }

    if (fallBackToJFileChooser) {
        AwtWindow(
            create = {
                object : Dialog(parent, /* modal = */ true) {

                    override fun setVisible(value: Boolean) {
                        if (value) {
                            val path = JFileChooser().apply {
                                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            }.takeIf { chooser ->
                                chooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION
                            }?.selectedFile?.absolutePath
                            onDirectorySelected(path)
                        } else {
                            super.setVisible(false)
                        }
                    }
                }
            },
            dispose = Dialog::dispose
        )
    }
}

/* file selected                      -> non-empty list
 * file not selected but dialog shown -> empty list
 * dialog not shown                   -> null
 */

private suspend fun readPowerShellScriptOutputLines(script: String): List<String>? {
    val command = script.replace('\n', ' ').replace(Regex(" {2,}"), " ")
    return exec("powershell Invoke-Expression -Command '$command'")
}

private suspend fun readShellCommandOutputLines(command: String): List<String>? = exec(command)
