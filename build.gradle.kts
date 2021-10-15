import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

val protobufVersion by extra("3.18.1")

plugins {
    kotlin("jvm") version "1.5.21"
    id("org.jetbrains.compose") version "1.0.0-alpha3"
    id("com.google.protobuf") version "0.8.17"
    id("app.cash.licensee") version "1.2.0"
}

repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${getKotlinPluginVersion()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")
    implementation("com.squareup:kotlinpoet:1.10.1")

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

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
}

tasks.test {
    useJUnitPlatform()
}
