plugins {
    kotlin("jvm") version "1.8.20"
}

group = "net.bladehunt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("net.kyori:adventure-text-serializer-gson:4.13.1")
    implementation("com.github.Minestom:Minestom:8ad2c7701f")
}

kotlin {
    jvmToolchain(17)
}