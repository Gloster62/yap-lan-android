package com.gloster.yaplan.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.gloster.yaplan.YapLanApp
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The buttons on reminder notifications; each one also clears the notification it was pressed on. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id == -1L) return
        val app = context.applicationContext as YapLanApp
        val notifications = NotificationManagerCompat.from(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TASK_DONE -> {
                        notifications.cancel(id.toInt())
                        app.repository.setDone(id, true)
                    }
                    ACTION_TASK_SNOOZE -> {
                        notifications.cancel(id.toInt())
                        app.scheduler.snooze(id, System.currentTimeMillis() + ReminderScheduler.SNOOZE_MILLIS)
                    }
                    ACTION_TASK_DISMISS -> {
                        notifications.cancel(id.toInt())
                        app.scheduler.cancelFollowUp(id)
                    }
                    ACTION_HABIT_DONE -> {
                        notifications.cancel(HABIT_TAG, id.toInt())
                        app.habits.setDone(id, LocalDate.now(), true)
                    }
                    ACTION_HABIT_SNOOZE -> {
                        notifications.cancel(HABIT_TAG, id.toInt())
                        app.scheduler.snoozeHabit(id, System.currentTimeMillis() + ReminderScheduler.SNOOZE_MILLIS)
                    }
                    ACTION_HABIT_DISMISS -> notifications.cancel(HABIT_TAG, id.toInt())
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Habit notifications share ids with task ones; the tag keeps them apart. */
        const val HABIT_TAG = "habit"

        private const val EXTRA_ID = "id"
        private const val ACTION_TASK_DONE = "com.gloster.yaplan.TASK_DONE"
        private const val ACTION_TASK_SNOOZE = "com.gloster.yaplan.TASK_SNOOZE"
        private const val ACTION_TASK_DISMISS = "com.gloster.yaplan.TASK_DISMISS"
        private const val ACTION_HABIT_DONE = "com.gloster.yaplan.HABIT_DONE"
        private const val ACTION_HABIT_SNOOZE = "com.gloster.yaplan.HABIT_SNOOZE"
        private const val ACTION_HABIT_DISMISS = "com.gloster.yaplan.HABIT_DISMISS"

        fun taskDone(context: Context, taskId: Long) = pending(context, ACTION_TASK_DONE, taskId)
        fun taskSnooze(context: Context, taskId: Long) = pending(context, ACTION_TASK_SNOOZE, taskId)
        fun taskDismiss(context: Context, taskId: Long) = pending(context, ACTION_TASK_DISMISS, taskId)
        fun habitDone(context: Context, habitId: Long) = pending(context, ACTION_HABIT_DONE, habitId)
        fun habitSnooze(context: Context, habitId: Long) = pending(context, ACTION_HABIT_SNOOZE, habitId)
        fun habitDismiss(context: Context, habitId: Long) = pending(context, ACTION_HABIT_DISMISS, habitId)

        /** The action makes each button its own PendingIntent although they share the request code. */
        private fun pending(context: Context, action: String, id: Long): PendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, NotificationActionReceiver::class.java).setAction(action).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
