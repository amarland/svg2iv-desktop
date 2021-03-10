package com.amarland.svg2iv

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.ExperimentalLayout
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.unit.IntSize
import com.amarland.svg2iv.ui.MainWindowContent
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalLayout
@ExperimentalMaterialApi
fun main() = Window(
    title = "svg2iv",
    size = IntSize(800, 350),
    resizable = false
) {
    MainWindowContent(MainWindowBloc())
}
