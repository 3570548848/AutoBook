package com.example.autobook.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object NotificationAccess {

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false

        return flat.split(":").any { entry ->
            ComponentName.unflattenFromString(entry)?.packageName == context.packageName
        }
    }
}
