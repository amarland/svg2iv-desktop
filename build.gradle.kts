import com.github.jk1.license.render.JsonReportRenderer
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version libs.versions.kotlin.asProvider().get()
    alias(libs.plugins.compose)
    alias(libs.plugins.licensee)
    alias(libs.plugins.dependency.license.report)
    alias(libs.plugins.ksp)
}

sourceSets.main.configure {
    java.srcDir("build/generated/ksp/main/kotlin")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlin.reflect)
    implementation(libs.coroutines.jdk8)
    implementation(libs.coroutines.swing)
    implementation(libs.kotlinpoet)
    implementation(libs.moshi)
    implementation(libs.appdirs)
    implementation(libs.cbor)

    // https://github.com/material-foundation/material-color-utilities/tree/main/java
    implementation(files("libs/material-color-utilities.jar"))

    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlin.compile.testing)

    ksp(libs.moshi.codegen)
}

compose.desktop {
    application {
        mainClass = "com.amarland.svg2iv.Main"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "svg2iv-desktop"
            packageVersion = "1.0.0"
        }
    }
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allowUrl("http://json.org/license.html")
    ignoreDependencies("org.jetbrains.compose.animation")
    ignoreDependencies("org.jetbrains.compose.foundation")
    ignoreDependencies("org.jetbrains.compose.material")
    ignoreDependencies("org.jetbrains.compose.runtime")
    ignoreDependencies("org.jetbrains.compose.ui")
    ignoreDependencies("org.jetbrains.skiko")
    ignoreDependencies("net.java.dev.jna")
}

licenseReport {
    val resourcesDirectoryPath = sourceSets.main.get()
        .resources.srcDirs
        .first().absolutePath
    outputDir = "$resourcesDirectoryPath/license-report"
    renderers = arrayOf(JsonReportRenderer("license-report.json", false))
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

// apply(from = "prebuild.gradle")

val configuration: Task.() -> Unit = {
    dependsOn(tasks.generateLicenseReport)
}
tasks.build.configure(configuration)
tasks.processResources.configure(configuration)
