package com.amarland.svg2iv.state

import com.amarland.svg2iv.outerworld.readProperties
import com.amarland.svg2iv.outerworld.writeProperties
import kotlin.reflect.KProperty

private const val KEY_USE_DARK_MODE = "use_dark_mode"

var useDarkMode by PropertiesDelegate(
    KEY_USE_DARK_MODE,
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
        writeProperties(properties.apply { setProperty(key, value.toString()) })
    }

    private companion object {

        private val properties by lazy(LazyThreadSafetyMode.NONE, ::readProperties)
    }
}
