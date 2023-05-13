@file:JvmName("Main")

package com.amarland.svg2iv

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.amarland.svg2iv.state.MainWindowBloc
import com.amarland.svg2iv.ui.LocalComposeWindow
import com.amarland.svg2iv.ui.MainWindowContent
import com.amarland.svg2iv.util.ShortcutKey

private lateinit var mainWindowBloc: MainWindowBloc

fun main() = singleWindowApplication(
    state = WindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        width = 800.dp,
        height = 360.dp
    ),
    title = "svg2iv",
    icon = useResource("logo.svg") { stream -> loadSvgPainter(stream, Density(1F)) },
    resizable = false,
    onPreviewKeyEvent = ::onPreviewKeyEvent
) {
    val coroutineScope = rememberCoroutineScope()
    remember {
        MainWindowBloc(coroutineScope).also { bloc ->
            mainWindowBloc = bloc
        }
    }

    CompositionLocalProvider(LocalComposeWindow provides window) {
        MainWindowContent(mainWindowBloc)
    }
}

fun onPreviewKeyEvent(event: KeyEvent): Boolean {
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
