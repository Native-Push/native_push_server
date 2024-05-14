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
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.interaso.webpush.VapidKeys
import com.interaso.webpush.WebPush
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.guava.await
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.net.ssl.SSLException
import kotlin.io.path.Path


abstract class NativePush<ID> {
    private var initialized = false
    private lateinit var apnsClient: ApnsClient

    private lateinit var webPush: WebPush

    @Throws(IOException::class, IllegalStateException::class, SSLException::class, NoSuchAlgorithmException::class,
        IllegalArgumentException::class, SecurityException::class, FileNotFoundException::class, GeneralSecurityException::class)
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

        if (pushSystems.contains(PushSystem.FCM)) {
            if (firebaseServiceAccountFile != null) {
                val serviceAccount = FileInputStream(firebaseServiceAccountFile)

                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

                FirebaseApp.initializeApp(options)
            }
            else {
                throw IllegalArgumentException("firebaseServiceAccountFile must be specified when using FCN")
            }
        }

        if (pushSystems.contains(PushSystem.APNS)) {
            val apnsClientBuilder = ApnsClientBuilder()

            if (apnsTopic != null) {
                apnsClientBuilder.withDefaultTopic(apnsTopic)
            }
            else {
                throw IllegalArgumentException("apnsTopic must be specified when using APNS")
            }

            if (apnsP12File != null && apnsP12Password != null) {
                apnsClientBuilder
                    .withCertificate(File(apnsP12File).inputStream())
                    .withPassword(apnsP12Password)
            }
            else if (apnsP8File != null && apnsKeyId != null && apnsTeamId != null) {
                val apnsAuthKey = File(apnsP8File).readLines().toMutableList()
                apnsAuthKey.removeFirst()
                apnsAuthKey.removeLast()
                apnsClientBuilder
                    .withApnsAuthKey(apnsAuthKey.joinToString(""))
                    .withKeyID(apnsKeyId)
                    .withTeamID(apnsTeamId)
            }
            else {
                throw IllegalArgumentException("Either apnsP12File and apnsP12Password or " +
                        "apnsP8File, apnsKeyId and apnsTeamId must be specified when using APNS")
            }
            if (development) {
                apnsClientBuilder.withDevelopmentGateway()
            }
            else {
                apnsClientBuilder.withProductionGateway()
            }
            apnsClient = apnsClientBuilder
                .inAsynchronousMode()
                .build()
        }

        if (pushSystems.contains(PushSystem.WEBPUSH)) {
            if (webPushSubject == null) {
                throw IllegalArgumentException("webPushSubject must be specified when using web push")
            }
            val vapidKeys = if (vapidKeysFile != null) {
                VapidKeys.load(Path(vapidKeysFile))
            }
            else if (vapidPublicKey != null && vapidPrivateKey != null) {
                VapidKeys.fromUncompressedBytes(vapidPublicKey, vapidPrivateKey)
            }
            else {
                throw IllegalArgumentException("vapidKeyFile or vapidPublicKey and vapidPrivateKey must be specified when using web push")
            }
            webPush = WebPush(webPushSubject, vapidKeys)
        }
    }

    @Throws(IllegalArgumentException::class, FirebaseMessagingException::class, JsonParseException::class,
        JsonSyntaxException::class, IllegalStateException::class, UnsupportedOperationException::class,
        GeneralSecurityException::class, IOException::class)
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
                    data ?: emptyMap(),
                )
            }
            success = success && tokenSuccess
        }
        return success
    }

    protected abstract suspend fun loadNotificationTokens(userId: ID): Iterable<Pair<String, PushSystem>>

    @Throws(IllegalStateException::class)
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
        apnsClient.push(notification, object : NotificationResponseListener {
            override fun onSuccess(notification: Notification?) {
                result.complete(true)
            }

            override fun onFailure(notification: Notification?, response: NotificationResponse?) {
                result.complete(false)
            }
        })
        return result.await()
    }

    /**
     * @throws IllegalArgumentException If any of the parameters are invalid
     * @throws FirebaseMessagingException If an error occurs while handing the message off to FCM for delivery
     */
    @Throws(IllegalArgumentException::class, FirebaseMessagingException::class)
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
        val androidNotification = AndroidNotification.builder()
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
        val androidConfig = AndroidConfig.builder()
            .setCollapseKey(collapseKey)
            .setNotification(androidNotification)
            .putAllData(data)
            .build()

        val apsAlert = ApsAlert.builder()
            .setTitle(title)
            .setTitleLocalizationKey(titleLocalizationKey)
            .addAllTitleLocArgs(titleLocalizationArgs.toMutableList())
            .setBody(body)
            .setLocalizationKey(bodyLocalizationKey)
            .addAllLocalizationArgs(bodyLocalizationArgs.toMutableList())
            .build()
        val aps = Aps.builder()
            .setMutableContent(imageUrl != null)
            .setThreadId(collapseKey)
            .setSound(sound)
            .setAlert(apsAlert)
            .putAllCustomData(data)
            .build()
        val apnsFcmOptions = ApnsFcmOptions.builder()
            .setImage(imageUrl)
            .build()
        val apnsConfig = ApnsConfig.builder()
            .setAps(aps)
            .setFcmOptions(apnsFcmOptions)
            .build()

        val webData = mutableMapOf(
            "titleLocalizationKey" to titleLocalizationKey,
            "titleLocalizationArgs" to titleLocalizationArgs,
            "bodyLocalizationKey" to bodyLocalizationKey,
            "bodyLocalizationArgs" to bodyLocalizationArgs,
        )
        webData.putAll(data)
        val webPushNotification = WebpushNotification.builder()
            .setTitle(title)
            .setBody(body)
            .setImage(imageUrl)
            .setData(webData)
            .build()
        val webPushConfig = WebpushConfig.builder()
            .setNotification(webPushNotification)
            .build()

        val message = Message.builder()
            .setToken(token)
            .setAndroidConfig(androidConfig)
            .setApnsConfig(apnsConfig)
            .setWebpushConfig(webPushConfig)
            .build()
        ApiFutureToListenableFuture(FirebaseMessaging.getInstance().sendAsync(message)).await()
        return true
    }

    /**
     * @throws JsonParseException
     * @throws JsonSyntaxException
     * @throws IllegalStateException
     * @throws UnsupportedOperationException
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Throws(JsonParseException::class, JsonSyntaxException::class, IllegalStateException::class,
        UnsupportedOperationException::class, GeneralSecurityException::class, IOException::class)
    private suspend fun sendWebPushNotification(
        token: String,
        title: String?,
        titleLocalizationKey: String?,
        titleLocalizationArgs: Array<String>,
        body: String?,
        bodyLocalizationKey: String?,
        bodyLocalizationArgs: Array<String>,
        imageUrl: String?,
        data: Map<String, String>,
    ): Boolean {
        val tokenJson = JsonParser.parseString(token).asJsonObject
        val payload = mapOf(
            "title" to title,
            "titleLocalizationKey" to titleLocalizationKey,
            "titleLocalizationArgs" to titleLocalizationArgs,
            "body" to body,
            "bodyLocalizationKey" to bodyLocalizationKey,
            "bodyLocalizationArgs" to bodyLocalizationArgs,
            "imageUrl" to imageUrl,
        ).mapNotNull {
            val value = it.value
            if (value !== null) {
                it.key to value
            }
            else {
                null
            }
        }.toMap().toMutableMap()
        payload.putAll(data)
        val payloadJson = Gson().toJson(payload)

        val endpoint = tokenJson["endpoint"].asString
        val p256dh = tokenJson["p256dh"].asString
        val auth = tokenJson["auth"].asString

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
        val response = HttpClient.newHttpClient().sendAsync(request, BodyHandlers.ofString()).await()
        return response.statusCode() in 200..<300
    }
}