package com.opdehipt.native_push

/**
 * This enum defines the different push notification systems that can be used
 * to send notifications to users. Each value represents a specific notification
 * service.
 */
enum class PushSystem {
    /**
     * Apple Push Notification Service (APNS).
     * Used for sending notifications to iOS devices.
     */
    APNS,

    /**
     * Firebase Cloud Messaging (FCM).
     * Used for sending notifications to Android devices and web apps.
     */
    FCM,

    /**
     * Web Push.
     * Used for sending notifications to web browsers via the Push API.
     */
    WEBPUSH,
}
