package com.example.yourbagbuddy.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.yourbagbuddy.MainActivity
import com.example.yourbagbuddy.R

/**
 * Helper for packing reminder notifications.
 * Creates channel (Android O+), builds notification with "Open checklist" action.
 * Fails silently if permission denied (no crash). Log only in debug.
 */
object PackingNotificationHelper {

    private const val CHANNEL_ID = "packing_reminders"
    private const val CHANNEL_NAME = "Packing Reminders"
    private const val NOTIFICATION_ID_PREFIX = 2000

    /** Vibration pattern: no delay, vibrate 400ms, pause 200ms, vibrate 400ms (attention-grabbing). */
    private val VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400)

    /**
     * Ensure notification channel exists. Call once at app startup or before first notification.
     */
    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for unchecked packing list items"
            setShowBadge(true)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    /**
     * Show packing reminder notification. Only call when there are unchecked item names.
     * Uses a stable notification id per checklist so updates replace previous.
     *
     * @param checklistId trip/checklist id (used for PendingIntent and notification id)
     * @param uncheckedItemNames names to show in body (e.g. "Charger, Jacket, Medicines")
     */
    fun showPackingReminder(
        context: Context,
        checklistId: String,
        uncheckedItemNames: List<String>
    ) {
        createChannelIfNeeded(context)
        val body = if (uncheckedItemNames.isEmpty()) {
            "You have unchecked items on your packing list."
        } else {
            "You still haven't packed: ${uncheckedItemNames.take(10).joinToString(", ")}"
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TRIP_ID, checklistId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            checklistId.hashCode() and 0x7FFFFFFF,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_packing_reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(VIBRATION_PATTERN)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationIdFor(checklistId), notification)
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.d("PackingNotification", "Notification shown for checklistId=$checklistId")
            }
        } catch (e: SecurityException) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.w("PackingNotification", "Permission denied, cannot show notification", e)
            }
        }
    }

    private fun notificationIdFor(checklistId: String): Int =
        NOTIFICATION_ID_PREFIX + (checklistId.hashCode() and 0x7FFF)
}
