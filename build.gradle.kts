plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "com.forexware"
version = "1.0-SNAPSHOT"

application {
    mainClass = "com.home.playground.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.objenesis:objenesis:3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.home.playground*")
    }
}

kotlin {
    jvmToolchain(21)
}