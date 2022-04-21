import com.github.jk1.license.render.JsonReportRenderer
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
    id("app.cash.licensee") version "1.2.0"
    // id("com.autonomousapps.dependency-analysis") version "0.79.0"
    id("com.github.jk1.dependency-license-report") version "2.0"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")
    implementation("com.squareup:kotlinpoet:1.10.2")
    implementation("com.squareup.moshi:moshi:1.13.0")
    implementation("net.harawata:appdirs:1.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.5")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "svg2iv-desktop"
            packageVersion = "1.0.0"
        }
    }
}

licensee {
    allow("Apache-2.0")
    allow("BSD-3-Clause")
    ignoreDependencies("org.jetbrains.compose.animation")
    ignoreDependencies("org.jetbrains.compose.foundation")
    ignoreDependencies("org.jetbrains.compose.material")
    ignoreDependencies("org.jetbrains.compose.runtime")
    ignoreDependencies("org.jetbrains.compose.ui")
}

licenseReport {
    renderers = arrayOf(JsonReportRenderer())
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
        freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

apply(from = "prebuild.gradle")
