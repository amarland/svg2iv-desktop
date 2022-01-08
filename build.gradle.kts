import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val protobufVersion by extra("3.19.1")

plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0-beta5"
    id("com.google.protobuf") version "0.8.17"
    id("app.cash.licensee") version "1.2.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${getKotlinPluginVersion()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")
    implementation("com.squareup:kotlinpoet:1.10.2")
    implementation("net.harawata:appdirs:1.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.5")
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}

compose.desktop.application.mainClass = "MainKt"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
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

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        languageVersion = "1.5"
        apiVersion = "1.5"
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}
