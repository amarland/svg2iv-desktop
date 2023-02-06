import com.github.jk1.license.render.JsonReportRenderer
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.compose") version "1.3.0"
    id("app.cash.licensee") version "1.6.0"
    // id("com.autonomousapps.dependency-analysis") version "0.79.0"
    id("com.github.jk1.dependency-license-report") version "2.0"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

sourceSets.main.configure {
    java.srcDir("build/generated/ksp/main/kotlin")
}

dependencies {
    val moshiVersion = "1.13.0"
    val coroutinesVersion = "1.6.4"

    implementation(compose.desktop.currentOs)
    @OptIn(ExperimentalComposeLibrary::class)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    implementation("net.harawata:appdirs:1.2.1")
    implementation("com.google.iot.cbor:cbor:0.01.02")

    // https://github.com/material-foundation/material-color-utilities/tree/main/java
    implementation(files("libs/material-color-utilities.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")

    ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
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
