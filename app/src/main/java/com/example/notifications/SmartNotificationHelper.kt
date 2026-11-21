package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object SmartNotificationHelper {
    const val CHANNEL_ID = "fekr_sessions_channel"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكير جلسات التخاطب والتقييم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات ذكية لتذكير أولياء الأمور بمواعيد جلسات مركز فكر القادمة"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotificationNow(context: Context, title: String, body: String, notificationId: Int = 1001) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_fekr)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission may be revoked concurrently by the user.
        }
    }

    fun scheduleSessionReminder(
        context: Context,
        appointmentId: String,
        childName: String,
        serviceName: String,
        dateLabel: String,
        timeSlot: String,
        sessionEpochMs: Long,
        minutesBefore: Int
    ) {
        ensureChannel(context)
        val triggerAtMs = (sessionEpochMs - minutesBefore * 60_000L)
            .coerceAtLeast(System.currentTimeMillis() + 5_000L)

        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, "تذكير جلسة $serviceName - $childName")
            putExtra(
                EXTRA_BODY,
                "موعد الجلسة يوم $dateLabel الساعة $timeSlot في مركز فكر. يرجى الحضور قبل الموعد بـ 10 دقائق."
            )
            putExtra(EXTRA_NOTIF_ID, appointmentId.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        } catch (_: SecurityException) {
            // Exact/idle alarm scheduling may be restricted on some OS configurations.
        }
    }
}
