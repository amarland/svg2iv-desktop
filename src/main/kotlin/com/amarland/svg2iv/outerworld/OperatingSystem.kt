package com.amarland.svg2iv.outerworld

fun isOSWindows() = System.getProperty("os.name")
    ?.toLowerCase()
    ?.startsWith("windows")
    ?: false
