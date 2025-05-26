plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("org.example.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("software.amazon.awssdk:s3:2.25.11")
    implementation("software.amazon.awssdk:netty-nio-client:2.25.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}