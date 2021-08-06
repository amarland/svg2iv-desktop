package com.amarland.svg2iv

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.state.MainWindowEvent
import com.amarland.svg2iv.ui.MainWindowContent
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalComposeUiApi
private val mainWindowBloc = MainWindowBloc()

@ExperimentalComposeUiApi
@ExperimentalCoroutinesApi
fun main() = singleWindowApplication(
    title = "svg2iv",
    resizable = false,
    state = WindowState(width = 800.dp, height = 350.dp),
    onPreviewKeyEvent = ::onPreviewKeyEvent
) {
    MainWindowContent(mainWindowBloc)
}

@ExperimentalComposeUiApi
private fun onPreviewKeyEvent(event: KeyEvent): Boolean {
    if (event.isAltPressed) {
        MainWindowBloc.MNEMONIC_KEYS.singleOrNull { it == event.key }
            ?.let { key -> mainWindowBloc.addEvent(MainWindowEvent.MnemonicPressed(key)) }
        return true
    }
    return false
}
