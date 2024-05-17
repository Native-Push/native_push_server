package com.opdehipt

import com.opdehipt.native_push.NativePush
import com.opdehipt.native_push.PushSystem
import com.opdehipt.plugins.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*

internal sealed class IdType<T>(val nativePush: NativePush<T>) {
    abstract fun PipelineContext<*, ApplicationCall>.getUserId(): T?
    abstract suspend fun addToken(system: PushSystem, token: kotlin.String, userId: T): java.util.UUID
    abstract suspend fun updateToken(tokenId: java.util.UUID, system: PushSystem, token: kotlin.String, userId: T): Boolean
    abstract suspend fun deleteToken(tokenId: java.util.UUID, userId: T): Boolean

    data object Long: IdType<kotlin.Long>(NativePushKtorLong) {
        override fun PipelineContext<*, ApplicationCall>.getUserId() = call.parameters["userId"]?.toLongOrNull()
        override suspend fun addToken(system: PushSystem, token: kotlin.String, userId: kotlin.Long) =
            insertNotificationToken(system, token, userId)
        override suspend fun updateToken(tokenId: java.util.UUID, system: PushSystem, token: kotlin.String, userId: kotlin.Long) =
            updateNotificationToken(tokenId, system, token, userId)
        override suspend fun deleteToken(tokenId: java.util.UUID, userId: kotlin.Long) =
            deleteNotificationToken(tokenId, userId)
    }
    data object UUID: IdType<java.util.UUID>(NativePushKtorUUID) {
        override fun PipelineContext<*, ApplicationCall>.getUserId(): java.util.UUID? {
            val userIdString = call.parameters["userId"]
            return if (userIdString != null) {
                try {
                    java.util.UUID.fromString(userIdString)
                } catch (e: IllegalStateException) {
                    null
                }
            } else {
                null
            }
        }
        override suspend fun addToken(system: PushSystem, token: kotlin.String, userId: java.util.UUID) =
            insertNotificationToken(system, token, userId)
        override suspend fun updateToken(tokenId: java.util.UUID, system: PushSystem, token: kotlin.String, userId: java.util.UUID) =
            updateNotificationToken(tokenId, system, token, userId)
        override suspend fun deleteToken(tokenId: java.util.UUID, userId: java.util.UUID) =
            deleteNotificationToken(tokenId, userId)
    }
    data object String: IdType<kotlin.String>(NativePushKtorString) {
        override fun PipelineContext<*, ApplicationCall>.getUserId() = call.parameters["userId"]
        override suspend fun addToken(system: PushSystem, token: kotlin.String, userId: kotlin.String) =
            insertNotificationToken(system, token, userId)
        override suspend fun updateToken(tokenId: java.util.UUID, system: PushSystem, token: kotlin.String, userId: kotlin.String) =
            updateNotificationToken(tokenId, system, token, userId)
        override suspend fun deleteToken(tokenId: java.util.UUID, userId: kotlin.String) =
            deleteNotificationToken(tokenId, userId)
    }
}