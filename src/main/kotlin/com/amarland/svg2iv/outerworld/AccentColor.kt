package com.amarland.svg2iv.outerworld

private const val VALUE_NAME = "AccentColorMenu"

suspend fun getAccentColorInt(): Int? {
    if (IS_OS_WINDOWS) {
        return exec(
            "reg",
            "query",
            """HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Accent""",
            "/v",
            VALUE_NAME
        )?.singleOrNull { line -> VALUE_NAME in line }
            ?.substringAfter(" 0x")
            ?.toLongOrNull(16)
            ?.let { abgr ->
                (abgr and 0xFF00FF00) + (abgr and 0xFF shl 16) + (abgr and 0xFF0000 shr 16)
            }?.toInt()
    }
    return null
}
