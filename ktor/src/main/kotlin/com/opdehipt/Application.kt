package com.opdehipt

import com.opdehipt.plugins.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.io.IOException
import java.util.*

fun main() {
    val env = applicationEngineEnvironment {
        envConfig()
    }
    embeddedServer(Netty, env).start(wait = true)
}

private fun ApplicationEngineEnvironmentBuilder.envConfig() {
    module {
        module()
    }
    connector {
        host = "0.0.0.0"
        port = 80
    }
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
        "development" to                getEnvironmentVariable("DEVELOPMENT"),
    ).mapNotNull {
        val value = it.value
        if (value != null) {
            it.key to value
        } else {
            null
        }
    }

    config = MapApplicationConfig(map)
}

private fun Application.module() {
    val idTypeString = environment.config.propertyOrNull("idType")?.getString()
    val idType = when(idTypeString?.lowercase()) {
        "uuid" -> IdType.UUID
        "long" -> IdType.Long
        else -> IdType.String
    }

    configureNativePush(idType)
    configureMonitoring()
    configureSerialization()
    configureDatabases(idType)
    configureRouting(idType)
}

@Throws(SecurityException::class)
private fun getEnvironmentVariable(key: String, checkForFile: Boolean = true): String? {
    val valueByKey = System.getenv(key)
    return if (!valueByKey.isNullOrBlank()) {
        valueByKey.trim()
    }
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
