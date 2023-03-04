@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spotless)
}

group = "company.thewebhook.ingestor"
version = "0.0.1"
application {
    mainClass.set("company.thewebhook.ingestor.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
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
    implementation(libs.bundles.ktorCore)
    implementation(libs.bundles.ktorContentNegotiation)
    implementation(libs.bundles.ktorResources)
    implementation(libs.bundles.ktorCompression)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.micrometerMetrics)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.koin)
    testImplementation(libs.bundles.tests)
}