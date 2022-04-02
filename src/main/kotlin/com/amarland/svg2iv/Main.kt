package com.amarland.svg2iv

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.ui.LocalComposeWindow
import com.amarland.svg2iv.ui.MainWindowContent
import com.amarland.svg2iv.util.ShortcutKey
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalComposeUiApi
private val mainWindowBloc = MainWindowBloc()

@ExperimentalComposeUiApi
@ExperimentalCoroutinesApi
@ExperimentalMaterialApi
fun main() = singleWindowApplication(
    title = "svg2iv",
    resizable = false,
    state = WindowState(width = 800.dp, height = 350.dp),
    onPreviewKeyEvent = ::onPreviewKeyEvent
) {
    CompositionLocalProvider(LocalComposeWindow provides window) {
        MainWindowContent(mainWindowBloc)
    }
}

@ExperimentalComposeUiApi
private fun onPreviewKeyEvent(event: KeyEvent): Boolean {
    if (event.type == KeyEventType.KeyDown) {
        val key = ShortcutKey.newInstance(
            event.key,
            isAltPressed = event.awtEventOrNull?.isAltDown ?: false
        )
        MainWindowBloc.SHORTCUT_BINDINGS[key]
            ?.let { action ->
                action(mainWindowBloc)
                return true
            }
    }
    return false
}
