package com.opdehipt.plugins

import com.opdehipt.IdType
import com.opdehipt.native_push.NativePush
import com.opdehipt.native_push.PushSystem
import io.ktor.server.application.*
import java.util.*

/**
 * Configures native push notifications for the Ktor application.
 *
 * @param T the type of the user ID.
 * @param idType the type of the user ID (UUID, Long, or String).
 */
internal fun <T> Application.configureNativePush(idType: IdType<T>) {
    // Retrieve the list of push systems from the configuration, default to all available systems if not set
    val pushSystems = environment.config.propertyOrNull("pushSystems")?.getString()
        ?.split(',')?.map { PushSystem.valueOf(it) } ?: PushSystem.entries

    // Initialize the native push notification system with configuration parameters
    idType.nativePush.initialize(
        pushSystems = pushSystems.toSet(),
        firebaseServiceAccountFile = environment.config.propertyOrNull("firebaseServiceAccountFile")?.getString(),
        apnsP8File = environment.config.propertyOrNull("apnsP8File")?.getString(),
        apnsKeyId = environment.config.propertyOrNull("apnsKeyId")?.getString(),
        apnsTeamId = environment.config.propertyOrNull("apnsTeamId")?.getString(),
        apnsTopic = environment.config.propertyOrNull("apnsTopic")?.getString(),
        apnsP12File = environment.config.propertyOrNull("apnsP12File")?.getString(),
        apnsP12Password = environment.config.propertyOrNull("apnsP12Password")?.getString(),
        webPushSubject = environment.config.propertyOrNull("webPushSubject")?.getString(),
        vapidKeysFile = environment.config.propertyOrNull("vapidKeysFile")?.getString(),
        vapidPublicKey = environment.config.propertyOrNull("vapidPublicKey")?.getString(),
        vapidPrivateKey = environment.config.propertyOrNull("vapidPrivateKey")?.getString(),
        development = environment.config.propertyOrNull("development")
            ?.getString()?.lowercase()?.toBooleanStrictOrNull() ?: false,
    )
}

/**
 * Native push notification implementation for UUID user IDs.
 */
internal object NativePushKtorUUID : NativePush<UUID>() {
    /**
     * Loads notification tokens for a given user with UUID.
     *
     * @param userId the UUID of the user.
     * @return an iterable collection of pairs of tokens and their corresponding push systems.
     */
    override suspend fun loadNotificationTokens(userId: UUID): Iterable<Pair<String, PushSystem>> =
        getNotificationTokens(userId)
}

/**
 * Native push notification implementation for Long user IDs.
 */
internal object NativePushKtorLong : NativePush<Long>() {
    /**
     * Loads notification tokens for a given user with Long ID.
     *
     * @param userId the Long ID of the user.
     * @return an iterable collection of pairs of tokens and their corresponding push systems.
     */
    override suspend fun loadNotificationTokens(userId: Long): Iterable<Pair<String, PushSystem>> =
        getNotificationTokens(userId)
}

/**
 * Native push notification implementation for String user IDs.
 */
internal object NativePushKtorString : NativePush<String>() {
    /**
     * Loads notification tokens for a given user with String ID.
     *
     * @param userId the String ID of the user.
     * @return an iterable collection of pairs of tokens and their corresponding push systems.
     */
    override suspend fun loadNotificationTokens(userId: String): Iterable<Pair<String, PushSystem>> =
        getNotificationTokens(userId)
}
