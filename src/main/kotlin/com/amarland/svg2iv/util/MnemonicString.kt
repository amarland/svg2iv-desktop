package com.amarland.svg2iv.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

fun String.asMnemonic(charIndex: Int = 0): AnnotatedString {
    require(charIndex <= lastIndex)
    return AnnotatedString(
        this,
        spanStyles = listOf(
            AnnotatedString.Range(
                item = SpanStyle(textDecoration = TextDecoration.Underline),
                start = charIndex,
                end = charIndex + 1
            )
        )
    )
}
