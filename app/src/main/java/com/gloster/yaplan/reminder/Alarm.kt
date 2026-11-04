package com.gloster.yaplan.reminder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gloster.yaplan.R
import com.gloster.yaplan.data.ChecklistItem
import com.gloster.yaplan.data.Habit
import com.gloster.yaplan.data.Task
import com.gloster.yaplan.reminder.ReminderReceiver.Companion.withItemsLeft
import com.gloster.yaplan.ui.alarm.AlarmActivity

/**
 * A reminder that behaves like an alarm clock: full screen over the lock screen, ringing until it
 * is answered or [RING_MILLIS] pass. When the phone is in use Android shows it as a heads-up
 * notification instead, with the same buttons.
 */
object Alarm {
    private const val RING_MILLIS = 5 * 60_000L

    fun ringHabit(context: Context, habit: Habit, streak: Int) = ring(
        context,
        NotificationCompat.Builder(context, NotificationChannels.CHAIN_ALARM)
            .setContentTitle("Zincir alarmı")
            .setContentText(HabitReminderReceiver.reminderText(habit.name, streak))
            .addAction(0, "Yaptım", NotificationActionReceiver.habitDone(context, habit.id))
            .addAction(0, "10 dk ertele", NotificationActionReceiver.habitSnooze(context, habit.id))
            .addAction(0, "Kapat", NotificationActionReceiver.habitDismiss(context, habit.id)),
        AlarmActivity.habitIntent(context, habit.id),
        habit.id.toInt(),
        NotificationActionReceiver.HABIT_TAG,
    )

    /** Shares the task's notification id, so its buttons and [isNotificationShowing] work unchanged. */
    fun ringTask(context: Context, task: Task, items: List<ChecklistItem>) = ring(
        context,
        NotificationCompat.Builder(context, NotificationChannels.TASK_ALARM)
            .setContentTitle("Görev alarmı")
            .setContentText(task.title)
            .addAction(0, "Tamamlandı", NotificationActionReceiver.taskDone(context, task.id))
            .addAction(0, "10 dk ertele", NotificationActionReceiver.taskSnooze(context, task.id))
            .addAction(0, "Kapat", NotificationActionReceiver.taskDismiss(context, task.id))
            .withItemsLeft(task, items),
        AlarmActivity.taskIntent(context, task.id),
        task.id.toInt(),
        tag = null,
    )

    private fun ring(context: Context, builder: NotificationCompat.Builder, face: Intent, id: Int, tag: String?) {
        val screen = PendingIntent.getActivity(
            context,
            id,
            face,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(screen, true)
            .setContentIntent(screen)
            .setOngoing(true)
            .setTimeoutAfter(RING_MILLIS)
            .build()
        // Insistent: the alarm sound repeats until the notification goes away.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        NotificationManagerCompat.from(context).notify(tag, id, notification)
    }

    fun stopHabit(context: Context, habitId: Long) {
        NotificationManagerCompat.from(context).cancel(NotificationActionReceiver.HABIT_TAG, habitId.toInt())
    }

    fun stopTask(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }
}
