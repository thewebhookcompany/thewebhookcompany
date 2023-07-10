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

    implementation(libs.kotlinxDateTime)

    implementation(libs.koinSlf4j)

    implementation(libs.hikaricp)
    implementation(libs.postgresql)

    implementation(libs.kotlinCore)
    implementation(libs.kotlinCoroutines)
    implementation(libs.exposedJdbc)
    implementation(libs.exposedCore)
    implementation(libs.exposedDatetime)

    testImplementation(libs.kotlinJunit)
}