# Native Push Library

This Kotlin library provides an abstract class for handling native push notifications for different systems like APNS (Apple Push Notification Service), FCM (Firebase Cloud Messaging), and WebPush. The library allows for sending notifications to users on iOS, Android, and web platforms.

## Features

- Support for multiple push notification systems: APNS, FCM, and WebPush.
- Customizable notification parameters such as title, body, image, sound, and priority.
- Asynchronous operations using Kotlin coroutines.
- Easily extendable to add more push notification systems.

## Installation

Add the following dependencies to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.opdehipt.native-push:1.0.0")
}
```

## Usage

1. **Initialize the library**: Call the `initialize` method with the necessary configuration parameters for the push notification systems you want to use.

2. **Send notifications**: Use the `sendNotification` method to send notifications to a user by providing their user ID and the notification details.

### Initialization Example

```kotlin
val nativePush = object : NativePush<String>() {
    override suspend fun loadNotificationTokens(userId: String): Map<String, PushSystem> {
        // Load and return the notification tokens for the user from your database
        return mapOf(
            "APNS_TOKEN" to PushSystem.APNS,
            "FCM_TOKEN" to PushSystem.FCM,
            "WEBPUSH_TOKEN" to PushSystem.WEBPUSH
        )
    }
}

nativePush.initialize(
    pushSystems = setOf(PushSystem.APNS, PushSystem.FCM, PushSystem.WEBPUSH),
    firebaseServiceAccountFile = "path/to/firebaseServiceAccount.json",
    apnsP8File = "path/to/apns.p8",
    apnsTeamId = "YOUR_APNS_TEAM_ID",
    apnsKeyId = "YOUR_APNS_KEY_ID",
    apnsTopic = "YOUR_APNS_TOPIC",
    webPushSubject = "mailto:your-email@example.com",
    vapidKeysFile = "path/to/vapidKeys.json",
    development = true
)
```

You can also omit `pushSystems` if you want to use all systems.
It is also possible to specify `apnsP12File` and `apnsP12Password`
instead of `apnsP8File`, `apnsTeamId` and `apnsTopic`. You can
specify `vapidPublicKey` and `vapidPrivateKey` instead of `vapidKeysFile`.
Please see [Native Push Vapid](https://github.com/Native-Push/native_push_vapid)
for information about generating the keys. You only have to specify the
arguments for a push system if you are using the system.

Please use the documentation for reference about the APNS, FCM
or Web Push variables:
- [APNS P8](https://developer.apple.com/documentation/usernotifications/establishing-a-token-based-connection-to-apns)
- [APNS P12](https://developer.apple.com/documentation/usernotifications/establishing-a-certificate-based-connection-to-apns)
- [APNS Topic](https://developer.apple.com/documentation/usernotifications/sending-notification-requests-to-apns#Send-a-POST-request-to-APNs)
- [FCM Service Account File](https://firebase.google.com/docs/admin/setup#initialize_the_sdk_in_non-google_environments)
- [Web Push Subject](https://datatracker.ietf.org/doc/html/draft-thomson-webpush-vapid#section-2.1)

### Sending Notification Example

```kotlin
val success = nativePush.sendNotification(
    userId = "USER_ID",
    title = "Hello",
    body = "This is a test notification",
    imageUrl = "https://example.com/image.png",
    priority = NotificationPriority.HIGH,
    data = mapOf("key1" to "value1", "key2" to "value2")
)
```

## Customization

To use this library, extend the `NativePush` abstract class and implement the `loadNotificationTokens` method to fetch the user's notification tokens from your database or any other storage.

## Enum Classes

### NotificationPriority

Defines the priority levels for notifications:

- `MIN`
- `LOW`
- `DEFAULT`
- `HIGH`
- `MAX`

### PushSystem

Defines the supported push notification systems:

- `APNS`
- `FCM`
- `WEBPUSH`