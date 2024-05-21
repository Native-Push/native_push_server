package com.opdehipt.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

/**
 * Configures serialization for the Ktor application.
 * This function sets up content negotiation to use JSON serialization.
 */
internal fun Application.configureSerialization() {
    // Install the ContentNegotiation plugin to handle content negotiation
    install(ContentNegotiation) {
        // Use Kotlinx serialization with JSON format
        json()
    }
}
