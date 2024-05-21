package com.opdehipt.plugins

import com.opdehipt.IdType
import com.opdehipt.native_push.NotificationPriority
import com.opdehipt.native_push.PushSystem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.security.InvalidParameterException
import java.util.*

/**
 * Configures the routing for the Ktor application.
 *
 * @param T the type of the user ID.
 * @param idType the type of the user ID (UUID, Long, or String).
 * @param authorizationValidationUrl the URL used for authorization validation.
 */
internal fun <T> Application.configureRouting(idType: IdType<T>, authorizationValidationUrl: String?) {
    val client = HttpClient()
    routing {
        route("/{userId}") {
            /**
             * Retrieves the user ID from the request.
             *
             * @return the user ID.
             * @throws InvalidParameterException if the user ID is invalid.
             */
            @Throws(InvalidParameterException::class)
            fun PipelineContext<*, ApplicationCall>.getUser() =
                idType.run { getUserId() } ?: throw InvalidParameterException("Invalid user id")

            /**
             * Verifies the authorization of the request.
             *
             * @param userId the user ID as a string.
             * @throws InvalidParameterException if authorization verification fails.
             */
            @Throws(InvalidParameterException::class)
            suspend fun PipelineContext<Unit, ApplicationCall>.verifyAuthorization(userId: String) {
                if (authorizationValidationUrl != null) {
                    val authorization = call.request.headers[HttpHeaders.Authorization]
                    val response = client.post(authorizationValidationUrl) {
                        header(HttpHeaders.Authorization, authorization)
                        header("User-Id", userId)
                    }
                    if (!response.status.isSuccess() || !response.body<SuccessResult>().success) {
                        throw InvalidParameterException("Authorization verification failed")
                    }
                }
            }

            route("/token") {
                post {
                    val userId = getUser()
                    verifyAuthorization(userId.toString())
                    val request = call.receive<TokenRequest>()
                    val tokenId = idType.addToken(request.system, request.token, userId)
                    call.respond(NewTokenResponse(tokenId))
                }
                route("/{id}") {
                    /**
                     * Retrieves the token ID from the request parameters.
                     *
                     * @return the token ID.
                     * @throws InvalidParameterException if the token ID is missing or invalid.
                     */
                    @Throws(InvalidParameterException::class)
                    fun PipelineContext<*, ApplicationCall>.getTokenId(): UUID {
                        val idString = call.parameters["id"] ?: throw InvalidParameterException("Missing id")
                        return UUID.fromString(idString)
                    }

                    put {
                        val id = getTokenId()
                        val userId = getUser()
                        verifyAuthorization(userId.toString())
                        val request = call.receive<TokenRequest>()
                        val success = idType.updateToken(id, request.system, request.token, userId)
                        call.respond(SuccessResult(success))
                    }
                    delete {
                        val id = getTokenId()
                        val userId = getUser()
                        verifyAuthorization(userId.toString())
                        val success = idType.deleteToken(id, userId)
                        call.respond(SuccessResult(success))
                    }
                }
            }
            post("/send-notification") {
                val userId = getUser()
                val req = call.receive<SendNotificationRequest>()
                val success = idType.nativePush.sendNotification(
                    userId = userId,
                    title = req.title,
                    titleLocalizationKey = req.titleLocalizationKey,
                    titleLocalizationArgs = req.titleLocalizationArgs?.toTypedArray() ?: emptyArray(),
                    body = req.body,
                    bodyLocalizationKey = req.bodyLocalizationKey,
                    bodyLocalizationArgs = req.bodyLocalizationArgs?.toTypedArray() ?: emptyArray(),
                    imageUrl = req.imageUrl,
                    channelId = req.channelId,
                    sound = req.sound,
                    icon = req.icon,
                    collapseKey = req.collapseKey,
                    priority = req.priority ?: NotificationPriority.DEFAULT,
                    data = req.data,
                )
                call.respond(SuccessResult(success))
            }
        }
    }
}

/**
 * Data class representing a success result.
 *
 * @property success indicates whether the operation was successful.
 */
@Serializable
private data class SuccessResult(
    val success: Boolean,
)

/**
 * Data class representing a token request.
 *
 * @property token the notification token.
 * @property system the push notification system.
 */
@Serializable
private data class TokenRequest(
    val token: String,
    @Serializable(with = PushSystemSerializer::class)
    val system: PushSystem,
)

/**
 * Data class representing a response containing a new token ID.
 *
 * @property id the new token ID.
 */
@Serializable
private data class NewTokenResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

/**
 * Data class representing a request to send a notification.
 *
 * @property title the notification title.
 * @property titleLocalizationKey the key for the title localization.
 * @property titleLocalizationArgs the arguments for the title localization.
 * @property body the notification body.
 * @property bodyLocalizationKey the key for the body localization.
 * @property bodyLocalizationArgs the arguments for the body localization.
 * @property imageUrl the URL of the notification image.
 * @property channelId the ID of the notification channel.
 * @property sound the sound to be played with the notification.
 * @property icon the icon of the notification.
 * @property collapseKey the collapse key for the notification.
 * @property priority the priority of the notification.
 * @property data additional data to be sent with the notification.
 */
@Serializable
private data class SendNotificationRequest(
    val title: String? = null,
    val titleLocalizationKey: String? = null,
    val titleLocalizationArgs: List<String>? = null,
    val body: String? = null,
    val bodyLocalizationKey: String? = null,
    val bodyLocalizationArgs: List<String>? = null,
    val imageUrl: String? = null,
    val channelId: String? = null,
    val sound: String? = null,
    val icon: String? = null,
    val collapseKey: String? = null,
    @Serializable(with = NotificationPrioritySerializer::class)
    val priority: NotificationPriority? = null,
    val data: Map<String, String>? = null,
)

/**
 * Serializer for the PushSystem enum.
 */
private class PushSystemSerializer : KSerializer<PushSystem> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PushSystem", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = PushSystem.valueOf(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: PushSystem) = encoder.encodeString(value.name)
}

/**
 * Serializer for the NotificationPriority enum.
 */
private class NotificationPrioritySerializer : KSerializer<NotificationPriority> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotificationPriority", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = NotificationPriority.valueOf(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: NotificationPriority) = encoder.encodeString(value.name)
}

/**
 * Serializer for UUID.
 */
private class UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
}
