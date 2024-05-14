plugins {
    alias(libs.plugins.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    implementation(libs.firebase.admin) {
        exclude("com.google.guava", "guava")
    }
    implementation(libs.kotlinx.coroutines.guava) {
        exclude("com.google.guava", "guava")
    }
    implementation(libs.guava)
    implementation(libs.apns)
    implementation(libs.web.push)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
