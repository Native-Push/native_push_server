package com.opdehipt.native_push

import com.clevertap.apns.Notification
import com.google.firebase.messaging.AndroidNotification
import com.interaso.webpush.WebPush

private typealias AndroidPriority = AndroidNotification.Priority
private typealias APNSPriority = Notification.Priority

/**
 * This enum defines the possible priority levels for notifications. The priority affects
 * how notifications are displayed to the user and the urgency with which they are delivered.
 */
enum class NotificationPriority {
    /**
     * Lowest priority. Notifications will be displayed in a less prominent manner.
     */
    MIN,

    /**
     * Low priority. Notifications will be displayed with low prominence.
     */
    LOW,

    /**
     * Default priority. Notifications will be displayed with standard prominence.
     */
    DEFAULT,

    /**
     * High priority. Notifications will be displayed more prominently.
     */
    HIGH,

    /**
     * Maximum priority. Notifications will be displayed with the highest prominence and urgency.
     */
    MAX;

    /**
     * Converts the current priority to the corresponding Android notification priority.
     *
     * @return The Android notification priority.
     */
    internal fun toAndroid(): AndroidPriority = when (this) {
        MIN -> AndroidPriority.MIN
        LOW -> AndroidPriority.LOW
        DEFAULT -> AndroidPriority.DEFAULT
        HIGH -> AndroidPriority.HIGH
        MAX -> AndroidPriority.MAX
    }

    /**
     * Converts the current priority to the corresponding APNS notification priority.
     *
     * @return The APNS notification priority.
     */
    internal fun toAPNS(): APNSPriority = when (this) {
        // APNS has only two levels: Power Consideration and Immediate
        // Map MIN and LOW to Power Consideration
        MIN, LOW -> APNSPriority.POWERCONSIDERATION
        // Map DEFAULT, HIGH, and MAX to Immediate
        DEFAULT, HIGH, MAX -> APNSPriority.IMMEDIATE
    }

    /**
     * Converts the current priority to the corresponding Web Push notification priority.
     *
     * @return The Web Push notification priority.
     */
    internal fun toWebPush(): WebPush.Urgency = when (this) {
        // Web push has only four levels: VeryLow, Low, Normal and High
        // Map MIN to very low
        MIN -> WebPush.Urgency.VeryLow
        // Map MIN to low
        LOW -> WebPush.Urgency.Low
        // Map DEFAULT to Normal
        DEFAULT -> WebPush.Urgency.Normal
        // Map HIGH and MAX to High
        HIGH, MAX -> WebPush.Urgency.High
    }
}
