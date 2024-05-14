val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.opdehipt"
version = "0.0.1"

application {
    mainClass.set("com.opdehipt.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ktor.core)
    implementation(libs.ktor.netty)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.contentNegotiation)
    implementation(libs.ktor.serialization)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgres)
    implementation(libs.mysql)
    implementation(libs.mariadb)
    implementation(libs.logback)
    implementation(project(":lib"))
    testImplementation(libs.ktor.tests)
    testImplementation(libs.junit)
}
