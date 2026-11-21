package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SessionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(SmartNotificationHelper.EXTRA_TITLE)
            ?: "تذكير جلسة مركز فكر"
        val body = intent.getStringExtra(SmartNotificationHelper.EXTRA_BODY)
            ?: "لديك جلسة تخاطب قادمة اليوم في مركز فكر للقياس والتقويم النفسي."
        val notifId = intent.getIntExtra(SmartNotificationHelper.EXTRA_NOTIF_ID, 2001)
        SmartNotificationHelper.showNotificationNow(context, title, body, notifId)
    }
}
