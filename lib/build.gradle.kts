plugins {
    alias(libs.plugins.jvm)
    `java-library`
    alias(libs.plugins.dependency.license.report)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.guava)
    implementation(libs.apns)
    implementation(libs.web.push)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
