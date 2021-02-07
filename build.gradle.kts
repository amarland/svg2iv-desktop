import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.compose.compose

plugins {
    kotlin("jvm") version "1.4.21"
    id("org.jetbrains.compose") version "0.3.0-build139"
    id("com.google.protobuf") version "0.8.14"
}

repositories {
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.google.protobuf:protobuf-java:3.14.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin.coreLibrariesVersion}")
    implementation("com.squareup:kotlinpoet:1.7.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/java")
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
        artifact = "com.google.protobuf:protoc:3.8.0"
    }
}

tasks.test {
    useJUnitPlatform()
}
