package com.opdehipt.native_push

import com.clevertap.apns.Notification
import com.google.firebase.messaging.AndroidNotification

private typealias AndroidPriority = AndroidNotification.Priority
private typealias APNSPriority = Notification.Priority

enum class NotificationPriority {
    MIN,
    LOW,
    DEFAULT,
    HIGH,
    MAX;

    fun toAndroid() = when (this) {
        MIN -> AndroidPriority.MIN
        LOW -> AndroidPriority.LOW
        DEFAULT -> AndroidPriority.DEFAULT
        HIGH -> AndroidPriority.HIGH
        MAX -> AndroidPriority.MAX
    }

    fun toAPNS() = when (this) {
        MIN, LOW -> APNSPriority.POWERCONSIDERATION
        DEFAULT, HIGH, MAX -> APNSPriority.IMMEDIATE
    }
}