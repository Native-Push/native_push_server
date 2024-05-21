package com.opdehipt.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

/**
 * Configures monitoring for the Ktor application.
 * This function sets up call logging to log incoming requests.
 */
internal fun Application.configureMonitoring() {
    // Install the CallLogging plugin to log HTTP call information
    install(CallLogging) {
        // Set the logging level to INFO
        level = Level.INFO

        // Define a filter to log only requests whose path starts with "/"
        filter { call -> call.request.path().startsWith("/") }
    }
}
