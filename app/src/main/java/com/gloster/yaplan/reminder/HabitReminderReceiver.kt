package com.gloster.yaplan.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gloster.yaplan.DeepLinks
import com.gloster.yaplan.R
import com.gloster.yaplan.YapLanApp
import com.gloster.yaplan.data.Habit
import com.gloster.yaplan.data.currentStreak
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        val app = context.applicationContext as YapLanApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = app.habits.getHabit(habitId) ?: return@launch
                // A snooze is a one-off; only the daily alarm moves itself on to tomorrow.
                if (intent.action != ACTION_SNOOZE) app.scheduler.updateHabit(habit)
                val done = app.habits.doneDays(habitId)
                val today = LocalDate.now()
                if (today in done || !canNotify(context)) return@launch
                val streak = currentStreak(done, today)
                if (habit.reminderAlarm) Alarm.ringHabit(context, habit, streak) else notify(context, habit, streak)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, habit: Habit, streak: Int) {
        val open = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            DeepLinks.intent(context, DeepLinks.habit(habit.id)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.HABITS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zinciri kırma!")
            .setContentText(reminderText(habit.name, streak))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Bugün yaptım", NotificationActionReceiver.habitDone(context, habit.id))
            .build()
        NotificationManagerCompat.from(context).notify(NotificationActionReceiver.HABIT_TAG, habit.id.toInt(), notification)
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val ACTION_SNOOZE = "com.gloster.yaplan.SNOOZED_HABIT"

        fun reminderText(name: String, streak: Int): String =
            if (streak > 0) "$name · $streak günlük zincirin seni bekliyor" else "$name · bugün henüz yapılmadı"
    }
}
