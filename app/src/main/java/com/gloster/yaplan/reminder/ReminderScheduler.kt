package com.gloster.yaplan.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gloster.yaplan.data.Habit
import com.gloster.yaplan.data.Task
import com.gloster.yaplan.data.nextMoodBoundaryAt
import com.gloster.yaplan.data.nextReminderAt
import com.gloster.yaplan.data.nextRepeatAt
import com.gloster.yaplan.widget.WidgetRefreshReceiver
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Every dated task reminds at its due time; chosen weekdays add a repeating one until it is done. */
    fun update(task: Task) {
        val now = System.currentTimeMillis()
        val due = task.dueAt?.takeIf { !task.isDone && it > now }
        if (due == null) alarmManager.cancel(dueIntent(task.id)) else setAlarm(due, dueIntent(task.id))

        val repeat = if (task.isDone) null else nextRepeatAt(now, task.repeatDays, task.repeatMinutes, ZoneId.systemDefault())
        if (repeat == null) alarmManager.cancel(repeatIntent(task.id)) else setAlarm(repeat, repeatIntent(task.id))

        if (task.isDone) {
            alarmManager.cancel(snoozeIntent(task.id))
            cancelFollowUp(task.id)
            Alarm.stopTask(context, task.id)
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(dueIntent(taskId))
        alarmManager.cancel(repeatIntent(taskId))
        alarmManager.cancel(snoozeIntent(taskId))
        cancelFollowUp(taskId)
        Alarm.stopTask(context, taskId)
    }

    fun snooze(taskId: Long, at: Long) {
        cancelFollowUp(taskId)
        setAlarm(at, snoozeIntent(taskId))
    }

    /** A second nudge if the notification on [channel] is still sitting unanswered by then. */
    fun followUp(taskId: Long, channel: String) {
        setAlarm(System.currentTimeMillis() + FOLLOW_UP_MILLIS, followUpIntent(taskId, channel))
    }

    fun cancelFollowUp(taskId: Long) {
        alarmManager.cancel(followUpIntent(taskId, null))
    }

    fun updateHabit(habit: Habit) {
        val minutes = habit.reminderMinutes ?: return cancelHabit(habit.id)
        setAlarm(nextReminderAt(System.currentTimeMillis(), minutes, ZoneId.systemDefault()), habitIntent(habit.id))
    }

    fun snoozeHabit(habitId: Long, at: Long) {
        setAlarm(at, habitIntent(habitId, HabitReminderReceiver.ACTION_SNOOZE))
    }

    fun cancelHabit(habitId: Long) {
        alarmManager.cancel(habitIntent(habitId))
        alarmManager.cancel(habitIntent(habitId, HabitReminderReceiver.ACTION_SNOOZE))
        stopHabitAlarm(habitId)
    }

    fun stopHabitAlarm(habitId: Long) {
        Alarm.stopHabit(context, habitId)
    }

    /** One evening check for every chain; the receiver decides whether any is at risk. */
    fun scheduleChainSaver() {
        val at = nextReminderAt(System.currentTimeMillis(), ChainSaver.MINUTES, ZoneId.systemDefault())
        setAlarm(at, broadcast(Intent(context, ChainSaverReceiver::class.java), 0))
    }

    /**
     * Not waking the phone: a widget nobody is looking at can wait for the screen to turn on. A
     * short window instead of an exact time needs no permission; plain set() may drift by an hour.
     */
    fun scheduleWidgetRefresh() {
        val at = nextMoodBoundaryAt(System.currentTimeMillis(), ZoneId.systemDefault())
        alarmManager.setWindow(AlarmManager.RTC, at, WIDGET_REFRESH_WINDOW_MILLIS, broadcast(Intent(context, WidgetRefreshReceiver::class.java), 0))
    }

    private fun setAlarm(at: Long, intent: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun dueIntent(taskId: Long): PendingIntent = taskIntent(taskId, action = null)

    /** The actions keep these separate alarms from [dueIntent] for the same task. */
    private fun repeatIntent(taskId: Long): PendingIntent = taskIntent(taskId, ReminderReceiver.ACTION_REPEAT)

    private fun snoozeIntent(taskId: Long): PendingIntent = taskIntent(taskId, ReminderReceiver.ACTION_SNOOZE)

    private fun followUpIntent(taskId: Long, channel: String?): PendingIntent =
        taskIntent(taskId, ReminderReceiver.ACTION_FOLLOW_UP) { putExtra(ReminderReceiver.EXTRA_CHANNEL, channel) }

    private fun taskIntent(taskId: Long, action: String?, extras: Intent.() -> Unit = {}): PendingIntent = broadcast(
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            .apply(extras),
        taskId.toInt(),
    )

    private fun habitIntent(habitId: Long, action: String? = null): PendingIntent = broadcast(
        Intent(context, HabitReminderReceiver::class.java)
            .setAction(action)
            .putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId),
        habitId.toInt(),
    )

    private fun broadcast(intent: Intent, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val SNOOZE_MILLIS = 10 * 60_000L
        const val FOLLOW_UP_MILLIS = 30 * 60_000L
        private const val WIDGET_REFRESH_WINDOW_MILLIS = 5 * 60_000L
    }
}
