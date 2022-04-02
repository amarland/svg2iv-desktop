package com.amarland.svg2iv.util

import androidx.compose.ui.input.key.Key
import kotlin.math.absoluteValue

@JvmInline
@Suppress("unused")
value class ShortcutKey private constructor(private val value: Long) {

    val key get() = Key(value.absoluteValue)

    val isAltPressed get() = value < 0L

    companion object {

        @JvmStatic
        fun newInstance(key: Key, isAltPressed: Boolean = false) =
            ShortcutKey(if (isAltPressed) -key.keyCode else key.keyCode)
    }
}
