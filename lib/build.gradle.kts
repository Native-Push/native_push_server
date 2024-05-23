plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.dependency.license.report)
    `java-library`
    `maven-publish`
    signing
}

group = "com.opdehipt"
version = "1.0"

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "native-push"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = "Native Push"
                description = "This Kotlin library provides an abstract class for handling native push notifications for different systems like APNS (Apple Push Notification Service), FCM (Firebase Cloud Messaging), and WebPush. The library allows for sending notifications to users on iOS, Android, and web platforms."
                url = "https://github.com/Native-Push/native_push_server/tree/main/lib"
                licenses {
                    license {
                        name = "BSD 3-Clause License"
                    }
                }
                developers {
                    developer {
                        id = "sven"
                        name = "Sven Op de Hipt"
                        email = "native-push@opdehipt.com"
                    }
                }
                scm {
                    url = "scm:git:https://github.com/Native-Push/native_push_server"
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
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
