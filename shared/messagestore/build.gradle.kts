@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spotless)
}

group = "company.thewebhook"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

spotless {
    kotlin {
        ktfmt(libs.versions.ktfmtVersion.get()).kotlinlangStyle()
    }
}

tasks.build {
    dependsOn(tasks.spotlessApply)
}

dependencies {
    implementation("company.thewebhook:util")

    implementation(libs.kotlinCore)
    implementation(libs.kafkaClients)
    implementation(libs.pulsarClient)
    implementation(libs.kotlinCoroutines)
    implementation(libs.kotlinxDateTime)
    implementation(libs.kotlinSerializationJson)
    implementation(libs.logback)
    testImplementation(libs.kotlinJunit)
}