package com.opdehipt

import com.opdehipt.plugins.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Entry point of the application. Configures and starts the Ktor server.
 */
fun main() {
    val env = applicationEngineEnvironment {
        envConfig()
    }
    embeddedServer(Netty, env).start(wait = true)
}

/**
 * Configures the application engine environment.
 * Sets up the Ktor application module and server connector.
 */
private fun ApplicationEngineEnvironmentBuilder.envConfig() {
    // Define the main application module
    module {
        module()
    }
    // Configure server connector
    connector {
        host = "0.0.0.0"
        port = 80
    }
    // Map environment variables to application configuration properties
    val map = mapOf(
        "idType" to                     getEnvironmentVariable("ID_TYPE"),
        "postgresHost" to               getEnvironmentVariable("POSTGRES_HOST"),
        "postgresDb" to                 getEnvironmentVariable("POSTGRES_DB"),
        "postgresUser" to               getEnvironmentVariable("POSTGRES_USER"),
        "postgresPassword" to           getEnvironmentVariable("POSTGRES_PASSWORD"),
        "mysqlHost" to                  getEnvironmentVariable("MYSQL_HOST"),
        "mysqlDb" to                    getEnvironmentVariable("MYSQL_DB"),
        "mysqlUser" to                  getEnvironmentVariable("MYSQL_USER"),
        "mysqlPassword" to              getEnvironmentVariable("MYSQL_PASSWORD"),
        "mariaHost" to                  getEnvironmentVariable("MARIA_HOST"),
        "mariaDb" to                    getEnvironmentVariable("MARIA_DB"),
        "mariaUser" to                  getEnvironmentVariable("MARIA_USER"),
        "mariaPassword" to              getEnvironmentVariable("MARIA_PASSWORD"),
        "pushSystems" to                getEnvironmentVariable("PUSH_SYSTEMS"),
        "firebaseServiceAccountFile" to getEnvironmentVariable("FIREBASE_SERVICE_ACCOUNT_FILE", checkForFile = false),
        "apnsP8File" to                 getEnvironmentVariable("APNS_P8_FILE", checkForFile = false),
        "apnsKeyId" to                  getEnvironmentVariable("APNS_KEY_ID"),
        "apnsTeamId" to                 getEnvironmentVariable("APNS_TEAM_ID"),
        "apnsTopic" to                  getEnvironmentVariable("APNS_TOPIC"),
        "apnsP12File" to                getEnvironmentVariable("APNS_P12_FILE", checkForFile = false),
        "apnsP12Password" to            getEnvironmentVariable("APNS_P12_PASSWORD"),
        "webPushSubject" to             getEnvironmentVariable("WEB_PUSH_SUBJECT"),
        "vapidKeysFile" to              getEnvironmentVariable("VAPID_KEYS_FILE"),
        "vapidPublicKey" to             getEnvironmentVariable("VAPID_PUBLIC_KEY"),
        "vapidPrivateKey" to            getEnvironmentVariable("VAPID_PRIVATE_KEY"),
        "authorizationValidationUrl" to getEnvironmentVariable("AUTHORIZATION_VALIDATION_URL"),
        "development" to                getEnvironmentVariable("DEVELOPMENT"),
    ).mapNotNull {
        val value = it.value
        if (value != null) {
            it.key to value
        } else {
            null
        }
    }

    // Set application configuration using the mapped environment variables
    config = MapApplicationConfig(map)
}

/**
 * Defines the main application module.
 * Configures various plugins and settings based on environment properties.
 */
private fun Application.module() {
    // Determine the ID type based on environment configuration
    val idTypeString = environment.config.propertyOrNull("idType")?.getString()
    val idType = when(idTypeString?.lowercase()) {
        "uuid" -> IdType.UUID
        "long" -> IdType.Long
        else -> IdType.String
    }

    // Configure various application features
    configureNativePush(idType)
    configureMonitoring()
    configureSerialization()
    configureDatabases(idType)
    configureRouting(idType, environment.config.propertyOrNull("authorizationValidationUrl")?.getString())
}

/**
 * Retrieves an environment variable or its corresponding file content.
 *
 * @param key the name of the environment variable.
 * @param checkForFile whether to check for a corresponding file containing the value.
 * @return the value of the environment variable or the content of the file.
 * @throws SecurityException if there is a security violation.
 */
@Throws(SecurityException::class)
private fun getEnvironmentVariable(key: String, checkForFile: Boolean = true): String? {
    // Retrieve the value directly from the environment variable
    val valueByKey = System.getenv(key)
    return if (!valueByKey.isNullOrBlank()) {
        valueByKey.trim()
    }
    // Check for a file containing the value if the environment variable is not set
    else if (checkForFile) {
        val filePath = System.getenv("${key}_FILE")
        if (filePath != null) {
            try {
                val line = File(filePath).bufferedReader().use { it.readLine() }
                if (!line.isNullOrBlank()) {
                    line.trim()
                }
                else {
                    null
                }
            } catch (e: IOException) {
                null
            }
        }
        else {
            null
        }
    }
    else {
        null
    }
}
