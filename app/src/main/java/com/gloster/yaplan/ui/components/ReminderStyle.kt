package com.gloster.yaplan.ui.components

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.gloster.yaplan.ui.theme.Yap

/** Android 14 made full-screen alarms a permission the user can switch off per app. */
private fun canUseFullScreen(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

/** "Bildirim" or "Alarm", for tasks and habits alike. */
@Composable
fun ReminderStyle(alarm: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var showFullScreenInfo by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Nasıl hatırlatayım?")
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableBox(selected = !alarm, onClick = { onChange(false) }, modifier = Modifier.weight(1f), height = 44.dp) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Bildirim", fontWeight = FontWeight.SemiBold)
            }
            SelectableBox(
                selected = alarm,
                onClick = {
                    onChange(true)
                    if (!canUseFullScreen(context)) showFullScreenInfo = true
                },
                modifier = Modifier.weight(1f),
                height = 44.dp,
            ) {
                Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Alarm", fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            if (alarm) "Tam ekran çalar; susturmazsan 5 dakika boyunca çalar." else "Bildirim olarak gelir.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = Yap.TextMuted,
        )
    }

    if (showFullScreenInfo) {
        AlertDialog(
            onDismissRequest = { showFullScreenInfo = false },
            containerColor = Yap.DialogSurface,
            title = { Text("Tam ekran izni") },
            text = {
                Text(
                    "Alarmın kilit ekranında tam ekran açılması için “Tam ekran bildirimleri” iznini açman " +
                        "gerekiyor. İzin olmadan alarm, sesli bir bildirim olarak gelir.",
                    color = Yap.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFullScreenInfo = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                                .setData("package:${context.packageName}".toUri()),
                        )
                    }
                }) { Text("Ayarlara git", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showFullScreenInfo = false }) { Text("Şimdi değil", color = Yap.Text) }
            },
        )
    }
}
