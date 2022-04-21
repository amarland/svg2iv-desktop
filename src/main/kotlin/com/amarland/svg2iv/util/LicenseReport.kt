package com.amarland.svg2iv.util

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LicenseReport(val dependencies: List<Dependency>)

@JsonClass(generateAdapter = true)
data class Dependency(
    val moduleName: String,
    val moduleVersion: String,
    val moduleUrls: List<String>?,
    val moduleLicenses: List<License>?
)

@JsonClass(generateAdapter = true)
data class License(val moduleLicense: String, val moduleLicenseUrl: String)
