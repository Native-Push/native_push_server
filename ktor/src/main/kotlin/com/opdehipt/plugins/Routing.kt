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

internal fun <T> Application.configureRouting(idType: IdType<T>, authorizationValidationUrl: String?) {
    routing {
        route("/{userId}") {
            fun PipelineContext<*, ApplicationCall>.getUser() =
                idType.run { getUserId() } ?: throw InvalidParameterException("Invalid user id")

            suspend fun PipelineContext<Unit, ApplicationCall>.verifyAuthorization(userId: String) {
                if (authorizationValidationUrl != null) {
                    val authorization = call.request.headers[HttpHeaders.Authorization]
                    val response = HttpClient().post(authorizationValidationUrl) {
                        header(HttpHeaders.Authorization, authorization)
                        header("user_id", userId)
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
                    fun PipelineContext<*, ApplicationCall>.getTokenId(): UUID {
                        val idString = call.parameters["id"] ?: throw InvalidParameterException("Missing id")
                        return UUID.fromString(idString)
                    }

                    put {
                        val id = getTokenId()
                        val userId = getUser()
                        verifyAuthorization(userId.toString())
                        val request = call.receive<TokenRequest>()
                        idType.updateToken(id, request.system, request.token, userId)
                        call.respond("")
                    }
                    delete {
                        val id = getTokenId()
                        val userId = getUser()
                        verifyAuthorization(userId.toString())
                        idType.deleteToken(id, userId)
                        call.respond("")
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

@Serializable
private data class SuccessResult(
    val success: Boolean,
)

@Serializable
private data class TokenRequest(
    val token: String,
    @Serializable(with = PushSystemSerializer::class)
    val system: PushSystem,
)

@Serializable
private data class NewTokenResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

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

private class PushSystemSerializer : KSerializer<PushSystem> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PushSystem", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = PushSystem.valueOf(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: PushSystem) = encoder.encodeString(value.name)
}

private class NotificationPrioritySerializer : KSerializer<NotificationPriority> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotificationPriority", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = NotificationPriority.valueOf(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: NotificationPriority) = encoder.encodeString(value.name)
}

private class UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
}
