package com.amarland.svg2iv.state

import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KProperty

private const val KEY_IS_DARK_MODE_ENABLED = "is_dark_mode_enabled"

var isDarkModeEnabled by PropertiesDelegate(
    KEY_IS_DARK_MODE_ENABLED,
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
        try {
            Files.newOutputStream(FILE_PATH).use { outputStream ->
                with(properties) {
                    setProperty(key, value.toString())
                    store(outputStream, null)
                }
            }
        } catch (e: IOException) {
            // well...
        }
    }

    private companion object {

        private val FILE_PATH = Path.of(
            // aligned with Flutter implementation (`shared_preferences` package)
            AppDirsFactory.getInstance().getUserDataDir(
                "svg2iv-desktop", null, "com.amarland", true
            ),
            File.separator,
            "svg2iv.properties"
        )

        @JvmStatic
        private val properties by lazy(LazyThreadSafetyMode.NONE) {
            Properties().apply {
                try {
                    if (!Files.exists(FILE_PATH))
                        Files.createFile(FILE_PATH)
                    Files.newInputStream(FILE_PATH).use(::load)
                } catch (e: IOException) {
                    // well...
                }
            }
        }
    }
}
