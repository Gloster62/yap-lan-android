package com.gloster.yaplan.ui.edit

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.gloster.yaplan.YapLanApp
import com.gloster.yaplan.data.Category
import com.gloster.yaplan.data.nextCategoryColor
import com.gloster.yaplan.data.Priority
import com.gloster.yaplan.data.hasDay
import com.gloster.yaplan.ui.categories.CategoryEditorSheet
import com.gloster.yaplan.ui.components.CategoryDot
import com.gloster.yaplan.ui.components.ChoiceChip
import com.gloster.yaplan.ui.components.FieldLabel
import com.gloster.yaplan.ui.components.LabeledTextField
import com.gloster.yaplan.ui.components.PriorityBars
import com.gloster.yaplan.ui.components.ReminderStyle
import com.gloster.yaplan.ui.components.SelectableBox
import com.gloster.yaplan.ui.dayLabel
import com.gloster.yaplan.ui.dayName
import com.gloster.yaplan.ui.formatRepeat
import com.gloster.yaplan.ui.formatShortDate
import com.gloster.yaplan.ui.formatTime
import com.gloster.yaplan.ui.theme.PlexMono
import com.gloster.yaplan.ui.theme.Yap
import com.gloster.yaplan.ui.weekdayInitials
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private val presetTimes = listOf(9, 12, 14, 18, 20).map { LocalTime.of(it, 0) }

/** Shared by the "Yeni görev" sheet and the edit screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskForm(
    state: TaskEditState,
    categories: List<Category>,
    actions: TaskFormActions,
    modifier: Modifier = Modifier,
    fieldColor: Color = Yap.Background,
    autoFocusTitle: Boolean = false,
) {
    val context = LocalContext.current
    val scheduler = (context.applicationContext as YapLanApp).scheduler
    val today = remember { LocalDate.now() }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var timePickerFor by rememberSaveable { mutableStateOf<TimeTarget?>(null) }
    var showExactAlarmInfo by rememberSaveable { mutableStateOf(false) }
    var exactAlarmAsked by rememberSaveable { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    /** Without the permission reminders can come minutes late; explain that once per form. */
    fun checkExactAlarm() {
        if (!exactAlarmAsked && !scheduler.canScheduleExact()) {
            exactAlarmAsked = true
            showExactAlarmInfo = true
        }
    }

    fun setDueDate(date: LocalDate?) {
        actions.setDueDate(date)
        if (date != null) checkExactAlarm()
    }

    if (autoFocusTitle) {
        LaunchedEffect(Unit) { titleFocus.requestFocus() }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        LabeledTextField(
            label = "Başlık",
            value = state.title,
            onValueChange = actions::setTitle,
            fieldModifier = Modifier.focusRequester(titleFocus),
            placeholder = "Ne yapacaksın?",
            containerColor = fieldColor,
            isError = !state.isNew && !state.canSave,
            errorText = "Başlık boş olamaz.",
        )
        NoteEditor(
            note = state.note,
            items = state.items,
            newItemText = state.newItemText,
            actions = actions,
            fieldColor = fieldColor,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Öncelik")
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    SelectableBox(
                        selected = state.priority == p,
                        onClick = { actions.setPriority(p) },
                        modifier = Modifier.weight(1f),
                    ) {
                        PriorityBars(p)
                        Text(p.label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FieldLabel("Kategori")
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(selected = state.categoryId == null, onClick = { actions.setCategory(null) }) {
                    Text("Yok")
                }
                categories.forEach { c ->
                    ChoiceChip(selected = state.categoryId == c.id, onClick = { actions.setCategory(c.id) }) {
                        CategoryDot(Color(c.color), size = 9.dp)
                        Text(c.name)
                    }
                }
                NewCategoryChip(onClick = { showNewCategory = true })
            }
        }

        if (!state.repeats) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Son tarih")
                val date = state.dueDate
                val isOther = date != null && date != today && date != today.plusDays(1)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateOption("Yok", date == null) { setDueDate(null) }
                    DateOption("Bugün", date == today) { setDueDate(today) }
                    DateOption("Yarın", date == today.plusDays(1)) { setDueDate(today.plusDays(1)) }
                    DateOption(if (isOther) formatShortDate(date) else "Başka gün", isOther) { showDatePicker = true }
                }
            }

            if (state.hasDate) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Saat")
                    TimeChoices(state.dueTime, onPreset = actions::setDueTime, onOther = { timePickerFor = TimeTarget.DUE })
                    val dueAt = state.dueAt(ZoneId.systemDefault())
                    FormHint(
                        if (dueAt != null && dueAt <= System.currentTimeMillis()) {
                            "Bu saat geçti, hatırlatma gelmeyecek."
                        } else {
                            "${if (state.reminderAlarm) "Alarm" else "Bildirim"}: ${dayLabel(state.dueDate!!, today)} ${formatTime(state.dueTime)}"
                        },
                    )
                }
            }
        }

        RepeatPicker(
            state = state,
            onToggleDay = { day ->
                actions.toggleRepeatDay(day)
                if (!state.repeatDays.hasDay(day)) checkExactAlarm()
            },
            onPresetTime = actions::setRepeatTime,
            onOtherTime = { timePickerFor = TimeTarget.REPEAT },
        )

        if (state.hasDate || state.repeats) {
            ReminderStyle(alarm = state.reminderAlarm, onChange = actions::setReminderAlarm)
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = (state.dueDate ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        setDueDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("İptal") } },
            colors = DatePickerDefaults.colors(containerColor = Yap.DialogSurface),
        ) {
            DatePicker(
                state = dateState,
                colors = DatePickerDefaults.colors(containerColor = Yap.DialogSurface),
            )
        }
    }

    timePickerFor?.let { target ->
        val initial = if (target == TimeTarget.DUE) state.dueTime else state.repeatTime
        val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { timePickerFor = null },
            containerColor = Yap.DialogSurface,
            title = { Text("Saat seç") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val time = LocalTime.of(timeState.hour, timeState.minute)
                    if (target == TimeTarget.DUE) actions.setDueTime(time) else actions.setRepeatTime(time)
                    timePickerFor = null
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { timePickerFor = null }) { Text("İptal") } },
        )
    }

    if (showExactAlarmInfo) {
        AlertDialog(
            onDismissRequest = { showExactAlarmInfo = false },
            containerColor = Yap.DialogSurface,
            title = { Text("Tam zamanlı hatırlatma") },
            text = {
                Text(
                    "Hatırlatmanın tam zamanında gelmesi için “Alarmlar ve hatırlatıcılar” " +
                        "iznini açman gerekiyor. İzin olmadan bildirim birkaç dakika gecikebilir.",
                    color = Yap.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmInfo = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData("package:${context.packageName}".toUri()),
                        )
                    }
                }) { Text("Ayarlara git", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmInfo = false }) { Text("Şimdi değil", color = Yap.Text) }
            },
        )
    }

    if (showNewCategory) {
        CategoryEditorSheet(
            initial = Category(name = "", color = nextCategoryColor(categories)),
            onDismiss = { showNewCategory = false },
            onSave = {
                actions.createCategory(it)
                showNewCategory = false
            },
        )
    }
}

@Composable
private fun RowScope.DateOption(label: String, selected: Boolean, onClick: () -> Unit) {
    SelectableBox(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.weight(1f),
        height = 44.dp,
        horizontalPadding = 4.dp,
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun NewCategoryChip(onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(36.dp)
                .drawBehind {
                    drawRoundRect(
                        color = Yap.CheckboxBorder,
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())),
                        ),
                    )
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Yap.TextSecondary, modifier = Modifier.size(16.dp))
            Text("Yeni kategori", color = Yap.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private enum class TimeTarget { DUE, REPEAT }

@Composable
private fun TimeChoices(selected: LocalTime, onPreset: (LocalTime) -> Unit, onOther: () -> Unit) {
    val isCustom = selected !in presetTimes
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        presetTimes.forEach { t ->
            SelectableBox(
                selected = selected == t,
                onClick = { onPreset(t) },
                modifier = Modifier.weight(1f),
                height = 44.dp,
                horizontalPadding = 0.dp,
            ) {
                Text(formatTime(t), fontFamily = PlexMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        SelectableBox(
            selected = isCustom,
            onClick = onOther,
            modifier = Modifier.weight(1.2f),
            height = 44.dp,
            horizontalPadding = 0.dp,
        ) {
            if (isCustom) {
                Text(formatTime(selected), fontFamily = PlexMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text("Diğer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FormHint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Yap.TextMuted, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = Yap.TextMuted)
    }
}

/** Weekdays to be reminded on, at one time of day, until the task is done. */
@Composable
private fun RepeatPicker(
    state: TaskEditState,
    onToggleDay: (DayOfWeek) -> Unit,
    onPresetTime: (LocalTime) -> Unit,
    onOtherTime: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            FieldLabel("Düzenli hatırlat")
            Text(
                " (isteğe bağlı)",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                color = Yap.TextMuted,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DayOfWeek.entries.forEach { day ->
                SelectableBox(
                    selected = state.repeatDays.hasDay(day),
                    onClick = { onToggleDay(day) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = dayName(day) },
                    role = Role.Checkbox,
                    height = 44.dp,
                    horizontalPadding = 0.dp,
                ) {
                    Text(weekdayInitials[day.ordinal], fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
        if (state.repeats) {
            TimeChoices(state.repeatTime, onPreset = onPresetTime, onOther = onOtherTime)
            FormHint("${formatRepeat(state.repeatDays, state.repeatTime)} · görevi bitirince durur")
        } else {
            Text(
                "Seçtiğin günlerde hatırlatma gelir. Ör. her Pazartesi ve Perşembe.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Yap.TextFaint,
            )
        }
    }
}
