package com.amarland.svg2iv

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.Window
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.unit.IntSize
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.state.MainWindowEvent
import com.amarland.svg2iv.ui.MainWindowContent
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalMaterialApi
fun main() = Window(
    title = "svg2iv",
    size = IntSize(800, 350),
    resizable = false
) {
    val bloc = MainWindowBloc()

    MainWindowContent(bloc).also {
        with(LocalAppWindow.current.keyboard) {
            for (shortcut in MainWindowBloc.SHORTCUTS) {
                setShortcut(shortcut) {
                    bloc.addEvent(
                        MainWindowEvent.ShortcutActivated(shortcut)
                    )
                }
            }
        }
    }
}
