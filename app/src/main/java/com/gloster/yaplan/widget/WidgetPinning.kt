package com.gloster.yaplan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** What to tell the user when the launcher can't place a widget for us. */
const val ADD_WIDGET_BY_HAND = "Ana ekrana uzun bas → Widget'lar → YAP LAN! yolunu kullan."

/**
 * Asks the launcher to place a calendar widget for [habitId]. False when the launcher can't pin
 * widgets; then the user has to add it from the home screen's widget list. Without a habit the
 * widget asks for one when tapped.
 */
fun requestHabitWidget(context: Context, habitId: Long? = null): Boolean =
    requestPin(context, HabitWidgetReceiver::class.java, ACTION_HABIT, habitId)

/** Like [requestHabitWidget], for one task and its list. */
fun requestTaskWidget(context: Context, taskId: Long? = null): Boolean =
    requestPin(context, TaskWidgetReceiver::class.java, ACTION_TASK, taskId)

fun requestTaskListWidget(context: Context): Boolean =
    requestPin(context, TaskListWidgetReceiver::class.java, action = null, id = null)

/** Whether at least one widget of [receiver] is on a home screen. */
fun hasWidget(context: Context, receiver: Class<*>): Boolean =
    AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()

private fun requestPin(context: Context, receiver: Class<*>, action: String?, id: Long?): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    // Mutable: the launcher fills in EXTRA_APPWIDGET_ID of the widget it placed. The action keeps
    // a habit and a task with the same id from sharing one PendingIntent.
    val onPinned = if (action == null || id == null) {
        null
    } else {
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, WidgetPinnedReceiver::class.java).setAction(action).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }
    return manager.requestPinAppWidget(ComponentName(context, receiver), null, onPinned)
}

private const val ACTION_HABIT = "com.gloster.yaplan.PINNED_HABIT"
private const val ACTION_TASK = "com.gloster.yaplan.PINNED_TASK"
private const val EXTRA_ID = "id"

class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || id == -1L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_HABIT -> HabitWidget.assign(context, appWidgetId, id)
                    ACTION_TASK -> TaskWidget.assign(context, appWidgetId, id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
