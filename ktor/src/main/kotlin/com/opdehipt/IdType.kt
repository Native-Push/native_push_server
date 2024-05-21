package com.opdehipt

import com.opdehipt.native_push.NativePush
import com.opdehipt.native_push.PushSystem
import com.opdehipt.plugins.*
import io.ktor.server.application.*
import io.ktor.util.pipeline.*

/**
 * Represents an abstract class for handling different types of user IDs (Long, UUID, or String)
 * with their respective native push notification implementations.
 *
 * @param T the type of the user ID.
 * @property nativePush the native push notification implementation for the user ID type.
 */
internal sealed class IdType<T>(val nativePush: NativePush<T>) {
    /**
     * Retrieves the user ID from the request context.
     *
     * @return the user ID.
     */
    abstract fun PipelineContext<*, ApplicationCall>.getUserId(): T?

    /**
     * Adds a new notification token for the user.
     *
     * @param system the push notification system.
     * @param token the notification token.
     * @param userId the user ID.
     * @return the ID of the newly added token.
     */
    abstract suspend fun addToken(system: PushSystem, token: kotlin.String, userId: T): java.util.UUID

    /**
     * Updates an existing notification token for the user.
     *
     * @param tokenId the ID of the token to update.
     * @param system the push notification system.
     * @param token the new notification token.
     * @param userId the user ID.
     * @return true if the update was successful, false otherwise.
     */
    abstract suspend fun updateToken(tokenId: java.util.UUID, system: PushSystem, token: kotlin.String, userId: T): Boolean

    /**
     * Deletes a notification token for the user.
     *
     * @param tokenId the ID of the token to delete.
     * @param userId the user ID.
     * @return true if the deletion was successful, false otherwise.
     */
    abstract suspend fun deleteToken(tokenId: java.util.UUID, userId: T): Boolean

    /**
     * Implementation of [IdType] for handling Long type user IDs.
     */
    data object Long: IdType<kotlin.Long>(NativePushKtorLong) {
        override fun PipelineContext<*, ApplicationCall>.getUserId() = call.parameters["userId"]?.toLongOrNull()
        override suspend fun addToken(system: PushSystem, token: kotlin.String, userId: kotlin.Long) =
            insertNotificationToken(system, token, userId)
        override suspend fun updateToken(tokenId: java.util.UUID, system: PushSystem, token: kotlin.String, userId: kotlin.Long) =
            updateNotificationToken(tokenId, system, token, userId)
        override suspend fun deleteToken(tokenId: java.util.UUID, userId: kotlin.Long) =
            deleteNotificationToken(tokenId, userId)
    }

    /**
     * Implementation of [IdType] for handling UUID type user IDs.
     */
    data object UUID: IdType<java.util.UUID>(NativePushKtorUUID) {
        override fun PipelineContext<*, ApplicationCall>.getUserId(): java.util.UUID? {
            val userIdString = call.parameters["userId"]
            return if (userIdString != null) {
                try {
                    java.util.UUID.fromString(userIdString)
                }
                catch (e: IllegalArgumentException) {
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

    /**
     * Implementation of [IdType] for handling String type user IDs.
     */
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
