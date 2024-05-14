package com.opdehipt.plugins

import com.opdehipt.IdType
import com.opdehipt.native_push.NativePush
import com.opdehipt.native_push.PushSystem
import io.ktor.server.application.*
import java.util.*

internal fun <T> Application.configureNativePush(idType: IdType<T>) {
    val pushSystems = environment.config.propertyOrNull("pushSystems")?.getString()
        ?.split(',')?.map { PushSystem.valueOf(it) } ?: PushSystem.entries
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

internal object NativePushKtorUUID: NativePush<UUID>() {
    override suspend fun loadNotificationTokens(userId: UUID): Iterable<Pair<String, PushSystem>> =
        getNotificationTokens(userId)
}

internal object NativePushKtorLong: NativePush<Long>() {
    override suspend fun loadNotificationTokens(userId: Long): Iterable<Pair<String, PushSystem>> =
        getNotificationTokens(userId)
}

internal object NativePushKtorString: NativePush<String>() {
    override suspend fun loadNotificationTokens(userId: String): Iterable<Pair<String, PushSystem>> =
        getNotificationTokens(userId)
}