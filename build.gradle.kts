import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.compose.compose

plugins {
    kotlin("jvm") version "1.4.32"
    id("org.jetbrains.compose") version "0.4.0-build180"
    id("com.google.protobuf") version "0.8.15"
}

repositories {
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.google.protobuf:protobuf-java:3.15.6")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.3")
    implementation("com.squareup:kotlinpoet:1.7.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
        resources {
            srcDir("build/generated/third_party_licenses/resources")
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
        artifact = "com.google.protobuf:protoc:3.15.6"
    }
}

tasks.test {
    useJUnitPlatform()
}
