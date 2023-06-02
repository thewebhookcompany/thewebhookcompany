@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spotless)
}

group = "company.thewebhook"
version = "0.0.1"
application {
    mainClass.set("company.thewebhook.ingestor.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

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
    implementation("company.thewebhook:messagestore")
    implementation("company.thewebhook:util")

    implementation(libs.bundles.ktorCore)
    implementation(libs.bundles.ktorUtil)
    implementation(libs.bundles.ktorMetrics)

    implementation(libs.kotlinSerializationCbor)

    implementation(libs.kotlinxDateTime)

    implementation(libs.koinKtor)
    implementation(libs.koinSlf4j)

    implementation(libs.kafkaClients)
    testImplementation(libs.ktorServerTests)
    testImplementation(libs.kotlinJunit)
}