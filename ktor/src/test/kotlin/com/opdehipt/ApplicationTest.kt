package com.opdehipt

import com.opdehipt.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting(IdType.UUID)
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}
