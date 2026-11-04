package com.gloster.yaplan.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.content.ContextCompat

/** One channel per kind of reminder, so each can be silenced on its own in system settings. */
object NotificationChannels {
    /** The app's first and only channel; keeping its id keeps what the user already set for it. */
    const val TASKS = "reminders"
    const val REPEAT = "repeat_reminders"
    const val HABITS = "habit_reminders"
    const val CHAIN_SAVER = "chain_saver"
    const val CHAIN_ALARM = "chain_alarm"
    const val TASK_ALARM = "task_alarm"

    fun create(context: Context) {
        val channels = listOf(
            channel(TASKS, "Görev hatırlatmaları", "Son tarihi gelen görevler"),
            channel(REPEAT, "Düzenli hatırlatmalar", "Seçtiğin günlerde gelen görev hatırlatmaları"),
            channel(HABITS, "Alışkanlık hatırlatmaları", "Alışkanlığın günlük hatırlatması"),
            channel(CHAIN_SAVER, "Zincir kurtarıcı", "Gece yarısı kırılacak zincirler için akşam uyarısı"),
            alarmChannel(CHAIN_ALARM, "Zincir alarmı", "Alarm olarak seçilen alışkanlık hatırlatmaları"),
            alarmChannel(TASK_ALARM, "Görev alarmı", "Alarm olarak seçilen görev hatırlatmaları"),
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(channels)
    }

    private fun channel(id: String, name: String, description: String) =
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply { this.description = description }

    private fun alarmChannel(id: String, name: String, description: String) = channel(id, name, description).apply {
        val alarmSound = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmSound)
        enableVibration(true)
    }
}

fun canNotify(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun isNotificationShowing(context: Context, id: Int, tag: String? = null): Boolean =
    context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == id && it.tag == tag }
