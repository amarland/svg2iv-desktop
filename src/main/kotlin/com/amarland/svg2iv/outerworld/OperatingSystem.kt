package com.amarland.svg2iv.outerworld

private val OS_NAME = System.getProperty("os.name")?.toLowerCase() ?: ""

@JvmField
val IS_OS_WINDOWS = OS_NAME.startsWith("windows")

@JvmField
val IS_OS_MACOS = OS_NAME.startsWith("mac")
