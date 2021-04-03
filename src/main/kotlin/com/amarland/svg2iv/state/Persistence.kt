package com.amarland.svg2iv.state

import java.io.File
import java.io.IOException
import java.util.Properties
import kotlin.reflect.KProperty

var isDarkModeEnabled by PropertiesDelegate(
    "isDarkModeEnabled",
    fromString = java.lang.Boolean::parseBoolean,
    defaultValue = false
)

private class PropertiesDelegate<T>(
    private val key: String,
    private val fromString: (String) -> T,
    private val defaultValue: T
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        properties.getProperty(key)?.let(fromString) ?: defaultValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        File(FILE_NAME).outputStream().use { outputStream ->
            with(properties) {
                setProperty(key, value.toString())
                store(outputStream, null)
            }
        }
    }

    private companion object {

        private const val FILE_NAME = "svg2iv.properties"

        @JvmStatic
        private val properties by lazy(LazyThreadSafetyMode.NONE) {
            Properties().apply {
                File(FILE_NAME).takeIf { file ->
                    @Suppress("UnnecessaryVariable")
                    val doesFileExist =
                        try {
                            !file.createNewFile()
                        } catch (e: IOException) {
                            false
                        }
                    return@takeIf doesFileExist
                }?.inputStream()?.use(::load)
            }
        }
    }
}
