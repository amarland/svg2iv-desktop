package com.amarland.svg2iv.outerworld

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun openFileChooser(
    parent: Frame,
    mode: FileSystemEntitySelectionMode
): Array<File> {
    return if (mode == FileSystemEntitySelectionMode.SOURCE)
        FileDialog(parent).apply {
            file = "*.svg" // for Windows
            setFilenameFilter { _, name -> name.endsWith(".svg") } // for other OSes
            isMultipleMode = true
            isVisible = true
        }.files ?: emptyArray()
    else {
        with(JFileChooser()) {
            when (mode) {
                // not reachable, the multi-selection "chooser" is too confusing IMO
                FileSystemEntitySelectionMode.SOURCE -> {
                    fileFilter = FileNameExtensionFilter("SVG Files", "svg")
                    isMultiSelectionEnabled = true
                }
                FileSystemEntitySelectionMode.DESTINATION -> {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
            }
            showDialog(parent, "Select")
            return@with selectedFiles?.takeIf { it.isNotEmpty() }
                ?: selectedFile?.let { arrayOf(it) } ?: emptyArray()
        }
    }
}

enum class FileSystemEntitySelectionMode { SOURCE, DESTINATION }
