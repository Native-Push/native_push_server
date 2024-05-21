package com.opdehipt.native_push

import com.clevertap.apns.ApnsClient
import com.clevertap.apns.Notification
import com.clevertap.apns.NotificationResponse
import com.clevertap.apns.NotificationResponseListener
import com.clevertap.apns.clients.ApnsClientBuilder
import com.google.api.core.ApiFutureToListenableFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.interaso.webpush.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.guava.await
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import kotlin.io.path.Path

/**
 * Abstract class representing a push notification system that supports multiple push services.
 *
 * @param ID The type of the user identifier.
 */
abstract class NativePush<ID> {
    private var initialized = false // Tracks whether the push system has been initialized
    private lateinit var apnsClient: ApnsClient // APNS client for sending notifications to iOS devices
    private lateinit var webPush: WebPush // WebPush client for sending notifications to web browsers

    /**
     * Initializes the push notification system with the specified configurations.
     *
     * @param pushSystems The set of push systems to initialize.
     * @param firebaseServiceAccountFile Path to the Firebase service account file.
     * @param apnsP8File Path to the APNS P8 file.
     * @param apnsTeamId APNS team ID.
     * @param apnsKeyId APNS key ID.
     * @param apnsP12File Path to the APNS P12 file.
     * @param apnsP12Password Password for the APNS P12 file.
     * @param apnsTopic APNS topic.
     * @param webPushSubject Subject for web push notifications.
     * @param vapidKeysFile Path to the VAPID keys file.
     * @param vapidPublicKey VAPID public key.
     * @param vapidPrivateKey VAPID private key.
     * @param development Flag indicating if the system is in development mode.
     * @throws IllegalStateException If the native push system is already initialized.
     * @throws FileNotFoundException if FCM is used and the firebaseServiceAccountFile is not found.
     * @throws IllegalArgumentException if certain push systems are used any of the necessary arguments
     * is not specified or invalid.
     */
    @Throws(IllegalStateException::class, FileNotFoundException::class, IllegalArgumentException::class)
    fun initialize(
        pushSystems: Set<PushSystem> = PushSystem.entries.toSet(),
        firebaseServiceAccountFile: String? = null,
        apnsP8File: String? = null,
        apnsTeamId: String? = null,
        apnsKeyId: String? = null,
        apnsP12File: String? = null,
        apnsP12Password: String? = null,
        apnsTopic: String? = null,
        webPushSubject: String? = null,
        vapidKeysFile: String? = null,
        vapidPublicKey: String? = null,
        vapidPrivateKey: String? = null,
        development: Boolean = false,
    ) {
        if (initialized) {
            throw IllegalStateException("Already initialized")
        }
        initialized = true

        // Initialize Firebase Cloud Messaging (FCM)
        if (pushSystems.contains(PushSystem.FCM)) {
            if (firebaseServiceAccountFile != null) {
                val serviceAccount = FileInputStream(firebaseServiceAccountFile)

                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

                FirebaseApp.initializeApp(options)
            } else {
                throw IllegalArgumentException("firebaseServiceAccountFile must be specified when using FCM")
            }
        }

        // Initialize Apple Push Notification Service (APNS)
        if (pushSystems.contains(PushSystem.APNS)) {
            val apnsClientBuilder = ApnsClientBuilder()

            if (apnsTopic != null) {
                apnsClientBuilder.withDefaultTopic(apnsTopic)
            } else {
                throw IllegalArgumentException("apnsTopic must be specified when using APNS")
            }

            // Configure APNS client with either P12 certificate or P8 key
            if (apnsP12File != null && apnsP12Password != null) {
                apnsClientBuilder
                    .withCertificate(File(apnsP12File).inputStream())
                    .withPassword(apnsP12Password)
            } else if (apnsP8File != null && apnsKeyId != null && apnsTeamId != null) {
                val apnsAuthKey = File(apnsP8File).readLines().toMutableList()
                apnsAuthKey.removeFirst() // Remove the header line
                apnsAuthKey.removeLast() // Remove the footer line
                apnsClientBuilder
                    .withApnsAuthKey(apnsAuthKey.joinToString(""))
                    .withKeyID(apnsKeyId)
                    .withTeamID(apnsTeamId)
            } else {
                throw IllegalArgumentException("Either apnsP12File and apnsP12Password or " +
                        "apnsP8File, apnsKeyId and apnsTeamId must be specified when using APNS")
            }
            // Set the APNS gateway to development or production
            if (development) {
                apnsClientBuilder.withDevelopmentGateway()
            } else {
                apnsClientBuilder.withProductionGateway()
            }
            try {
                apnsClient = apnsClientBuilder
                    .inAsynchronousMode()
                    .build()
            }
            catch (e: Exception) {
                throw IllegalArgumentException("Error while creating the APNS client", e)
            }
        }

        // Initialize Web Push
        if (pushSystems.contains(PushSystem.WEBPUSH)) {
            if (webPushSubject == null) {
                throw IllegalArgumentException("webPushSubject must be specified when using web push")
            }
            val vapidKeys = if (vapidKeysFile != null) {
                VapidKeys.load(Path(vapidKeysFile))
            } else if (vapidPublicKey != null && vapidPrivateKey != null) {
                VapidKeys.fromUncompressedBytes(vapidPublicKey, vapidPrivateKey)
            } else {
                throw IllegalArgumentException("vapidKeyFile or vapidPublicKey and vapidPrivateKey must be specified when using web push")
            }
            webPush = WebPush(webPushSubject, vapidKeys)
        }
    }

    /**
     * Sends a notification to the specified user with the given parameters.
     *
     * @param userId The identifier of the user to send the notification to.
     * @param title The title of the notification.
     * @param titleLocalizationKey The localization key for the notification title.
     * @param titleLocalizationArgs The localization arguments for the notification title.
     * @param body The body of the notification.
     * @param bodyLocalizationKey The localization key for the notification body.
     * @param bodyLocalizationArgs The localization arguments for the notification body.
     * @param imageUrl The URL of the image to be displayed in the notification.
     * @param channelId The ID of the channel to send the notification through.
     * @param sound The sound to be played when the notification is received.
     * @param icon The icon to be displayed in the notification.
     * @param collapseKey The collapse key for the notification.
     * @param priority The priority of the notification.
     * @param data Additional data to be included in the notification.
     * @return true if the notification was sent successfully, false otherwise.
     * @throws IllegalArgumentException If any of the parameters are invalid.
     * @throws JsonSyntaxException If web push is used and the token is not valid JSON.
     */
    @Throws(IllegalArgumentException::class, JsonSyntaxException::class)
    suspend fun sendNotification(
        userId: ID,
        title: String? = null,
        titleLocalizationKey: String? = null,
        titleLocalizationArgs: Array<String> = emptyArray(),
        body: String? = null,
        bodyLocalizationKey: String? = null,
        bodyLocalizationArgs: Array<String> = emptyArray(),
        imageUrl: String? = null,
        channelId: String? = null,
        sound: String? = null,
        icon: String? = null,
        collapseKey: String? = null,
        priority: NotificationPriority = NotificationPriority.DEFAULT,
        data: Map<String, String>? = null,
    ): Boolean {
        val tokens = loadNotificationTokens(userId)
        var success = true
        // Iterate over the notification tokens and send the notification to each one
        for ((token, system) in tokens) {
            val tokenSuccess = when (system) {
                PushSystem.APNS -> sendAPNSNotification(
                    token,
                    title,
                    titleLocalizationKey,
                    titleLocalizationArgs,
                    body,
                    bodyLocalizationKey,
                    bodyLocalizationArgs,
                    imageUrl,
                    sound,
                    collapseKey,
                    priority,
                    data ?: emptyMap(),
                )
                PushSystem.FCM -> sendFCMNotification(
                    token,
                    title,
                    titleLocalizationKey,
                    titleLocalizationArgs,
                    body,
                    bodyLocalizationKey,
                    bodyLocalizationArgs,
                    imageUrl,
                    channelId,
                    sound,
                    icon,
                    collapseKey,
                    priority,
                    data ?: emptyMap(),
                )
                PushSystem.WEBPUSH -> sendWebPushNotification(
                    token,
                    title,
                    titleLocalizationKey,
                    titleLocalizationArgs,
                    body,
                    bodyLocalizationKey,
                    bodyLocalizationArgs,
                    imageUrl,
                    priority,
                    data ?: emptyMap(),
                )
            }
            // If any notification fails, mark the overall success as false
            success = success && tokenSuccess
        }
        return success
    }

    /**
     * Loads the notification tokens for the specified user.
     *
     * @param userId The identifier of the user to load the notification tokens for.
     * @return An iterable of pairs containing the token and the push system.
     */
    protected abstract suspend fun loadNotificationTokens(userId: ID): Iterable<Pair<String, PushSystem>>

    /**
     * Sends an APNS notification with the specified parameters.
     *
     * @param token The APNS token.
     * @param title The title of the notification.
     * @param titleLocalizationKey The localization key for the notification title.
     * @param titleLocalizationArgs The localization arguments for the notification title.
     * @param body The body of the notification.
     * @param bodyLocalizationKey The localization key for the notification body.
     * @param bodyLocalizationArgs The localization arguments for the notification body.
     * @param imageUrl The URL of the image to be displayed in the notification.
     * @param sound The sound to be played when the notification is received.
     * @param collapseKey The collapse key for the notification.
     * @param priority The priority of the notification.
     * @param data Additional data to be included in the notification.
     * @return true if the notification was sent successfully, false otherwise.
     */
    private suspend fun sendAPNSNotification(
        token: String,
        title: String?,
        titleLocalizationKey: String?,
        titleLocalizationArgs: Array<String>,
        body: String?,
        bodyLocalizationKey: String?,
        bodyLocalizationArgs: Array<String>,
        imageUrl: String?,
        sound: String?,
        collapseKey: String?,
        priority: NotificationPriority,
        data: Map<String, String>
    ): Boolean {
        // Build the APNS notification with provided parameters
        val notificationBuilder = Notification.Builder(token)
            .alertTitle(title)
            .alertTitleLocKey(titleLocalizationKey)
            .alertTitleLocArgs(titleLocalizationArgs)
            .alertBody(body)
            .alertLocKey(bodyLocalizationKey)
            .alertLocArgs(bodyLocalizationArgs)
            .sound(sound)
            .priority(priority.toAPNS())
            .collapseId(collapseKey)

        if (imageUrl != null) {
            notificationBuilder
                .mutableContent()
                .customField("native_push_image", imageUrl)
        }

        for ((key, value) in data) {
            notificationBuilder.customField(key, value)
        }

        val notification = notificationBuilder.build()

        val result = CompletableDeferred<Boolean>()
        // Send the notification using the APNS client
        apnsClient.push(notification, object : NotificationResponseListener {
            override fun onSuccess(notification: Notification?) {
                result.complete(true)
            }

            override fun onFailure(notification: Notification?, response: NotificationResponse?) {
                result.complete(false)
            }
        })
        // Await the response
        return result.await()
    }

    /**
     * Sends an FCM notification with the specified parameters.
     *
     * @param token The FCM token.
     * @param title The title of the notification.
     * @param titleLocalizationKey The localization key for the notification title.
     * @param titleLocalizationArgs The localization arguments for the notification title.
     * @param body The body of the notification.
     * @param bodyLocalizationKey The localization key for the notification body.
     * @param bodyLocalizationArgs The localization arguments for the notification body.
     * @param imageUrl The URL of the image to be displayed in the notification.
     * @param channelId The ID of the channel to send the notification through.
     * @param sound The sound to be played when the notification is received.
     * @param icon The icon to be displayed in the notification.
     * @param collapseKey The collapse key for the notification.
     * @param priority The priority of the notification.
     * @param data Additional data to be included in the notification.
     * @return true if the notification was sent successfully, false otherwise.
     * @throws IllegalArgumentException If any of the parameters are invalid.
     */
    @Throws(IllegalArgumentException::class)
    private suspend fun sendFCMNotification(
        token: String,
        title: String?,
        titleLocalizationKey: String?,
        titleLocalizationArgs: Array<String>,
        body: String?,
        bodyLocalizationKey: String?,
        bodyLocalizationArgs: Array<String>,
        imageUrl: String?,
        channelId: String?,
        sound: String?,
        icon: String?,
        collapseKey: String?,
        priority: NotificationPriority,
        data: Map<String, String>,
    ): Boolean {
        // Build the FCM message with provided parameters
        val message = Message.builder()
            .setToken(token)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setCollapseKey(collapseKey)
                    .setNotification(
                        AndroidNotification.builder()
                            .setTitle(title)
                            .setTitleLocalizationKey(titleLocalizationKey)
                            .addAllTitleLocalizationArgs(titleLocalizationArgs.toMutableList())
                            .setBody(body)
                            .setBodyLocalizationKey(bodyLocalizationKey)
                            .addAllBodyLocalizationArgs(bodyLocalizationArgs.toMutableList())
                            .setImage(imageUrl)
                            .setChannelId(channelId)
                            .setSound(sound)
                            .setIcon(icon)
                            .setPriority(priority.toAndroid())
                            .build()
                    )
                    .putAllData(data)
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setMutableContent(imageUrl != null)
                            .setThreadId(collapseKey)
                            .setSound(sound)
                            .setAlert(
                                ApsAlert.builder()
                                    .setTitle(title)
                                    .setTitleLocalizationKey(titleLocalizationKey)
                                    .addAllTitleLocArgs(titleLocalizationArgs.toMutableList())
                                    .setBody(body)
                                    .setLocalizationKey(bodyLocalizationKey)
                                    .addAllLocalizationArgs(bodyLocalizationArgs.toMutableList())
                                    .build()
                            )
                            .putAllCustomData(data)
                            .build()
                    )
                    .setFcmOptions(
                        ApnsFcmOptions.builder()
                            .setImage(imageUrl)
                            .build()
                    )
                    .build()
            )
            .setWebpushConfig(
                WebpushConfig.builder()
                    .setNotification(
                        WebpushNotification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setImage(imageUrl)
                            .setData(
                                mapOf(
                                    "titleLocalizationKey" to titleLocalizationKey,
                                    "titleLocalizationArgs" to titleLocalizationArgs,
                                    "bodyLocalizationKey" to bodyLocalizationKey,
                                    "bodyLocalizationArgs" to bodyLocalizationArgs,
                                    *data.toList().toTypedArray(),
                                )
                            )
                            .build()
                    )
                    .build()
            )
            .build()
        // Send the message using the FirebaseMessaging instance
        return try {
            ApiFutureToListenableFuture(FirebaseMessaging.getInstance().sendAsync(message)).await()
            true
        } catch (e: FirebaseMessagingException) {
            false
        }
    }

    /**
     * Sends a web push notification with the specified parameters.
     *
     * @param token The web push token.
     * @param title The title of the notification.
     * @param titleLocalizationKey The localization key for the notification title.
     * @param titleLocalizationArgs The localization arguments for the notification title.
     * @param body The body of the notification.
     * @param bodyLocalizationKey The localization key for the notification body.
     * @param bodyLocalizationArgs The localization arguments for the notification body.
     * @param imageUrl The URL of the image to be displayed in the notification.
     * @param data Additional data to be included in the notification.
     * @return true if the notification was sent successfully, false otherwise.
     * @throws JsonSyntaxException if the specified token is not valid JSON.
     * @throws IllegalArgumentException if the p256dh or the auth key aren't valid base64 strings.
     */
    @Throws(JsonSyntaxException::class, IllegalArgumentException::class)
    private suspend fun sendWebPushNotification(
        token: String,
        title: String?,
        titleLocalizationKey: String?,
        titleLocalizationArgs: Array<String>,
        body: String?,
        bodyLocalizationKey: String?,
        bodyLocalizationArgs: Array<String>,
        imageUrl: String?,
        priority: NotificationPriority,
        data: Map<String, String>,
    ): Boolean {
        // Create the payload for the web push notification
        val payload = mapOf(
            "title" to title,
            "titleLocalizationKey" to titleLocalizationKey,
            "titleLocalizationArgs" to titleLocalizationArgs,
            "body" to body,
            "bodyLocalizationKey" to bodyLocalizationKey,
            "bodyLocalizationArgs" to bodyLocalizationArgs,
            "image" to imageUrl,
        ).mapNotNull {
            val value = it.value
            if (value !== null) {
                it.key to value
            } else {
                null
            }
        }.toMap().toMutableMap()
        payload.putAll(data)
        val payloadJson = Gson().toJson(payload)

        // Parse the token to JSON and extract endpoint, p256dh and auth
        val tokenJson = try {
            JsonParser.parseString(token).asJsonObject
        } catch (e: IllegalStateException) {
            throw JsonSyntaxException("The token isn't a json object.")
        }
        val (endpoint, p256dh, auth) =
        try {
            Triple(
                tokenJson["endpoint"].asString,
                tokenJson["p256dh"].asString,
                tokenJson["auth"].asString
            )
        } catch (e: UnsupportedOperationException) {
            throw JsonSyntaxException("The token doesn't contain three json string values with the keys endpoint, p256dh and auth")
        } catch (e: IllegalStateException) {
            throw JsonSyntaxException("The token doesn't contain three json string values with the keys endpoint, p256dh and auth")
        }

        // Send the notification to the endpoint using the vapid keys.
        val reqHeaders = webPush.getHeaders(endpoint, null, null, null)
        val reqBody = webPush.getBody(
            payloadJson.toByteArray(),
            Base64.getUrlDecoder().decode(p256dh),
            Base64.getUrlDecoder().decode(auth),
        )

        val request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofByteArray(reqBody))
            .uri(URI.create(endpoint))
            .apply { reqHeaders.forEach { setHeader(it.key, it.value) } }
            .build()
        val response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        return response.statusCode() in 200..<300
    }
}