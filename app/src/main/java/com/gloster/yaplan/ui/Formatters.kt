package com.gloster.yaplan.ui

import com.gloster.yaplan.data.ChecklistItem
import com.gloster.yaplan.data.EVERY_DAY
import com.gloster.yaplan.data.WEEKDAYS
import com.gloster.yaplan.data.WEEKEND
import com.gloster.yaplan.data.days
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val turkish = Locale.forLanguageTag("tr")
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", turkish)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", turkish)
private val weekdayFormat = DateTimeFormatter.ofPattern("EEEE", turkish)
private val dayMonthFormat = DateTimeFormatter.ofPattern("d MMMM", turkish)
private val dayMonthYearFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", turkish)
private val shortDateFormat = DateTimeFormatter.ofPattern("d MMM", turkish)
private val monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy", turkish)

val weekdayInitials = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")

private fun String.capitalized() = replaceFirstChar { it.titlecase(turkish) }

fun Long.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

fun formatDateTime(millis: Long): String = millis.toLocalDateTime().format(dateTimeFormat)

fun formatTime(time: LocalTime): String = time.format(timeFormat)

/** "Pazartesi, 19 Ekim" */
fun formatHeaderDate(date: LocalDate): String =
    "${date.format(weekdayFormat).capitalized()}, ${date.format(dayMonthFormat)}"

/** "Ekim 2026" */
fun formatMonth(month: YearMonth): String = month.format(monthFormat).capitalized()

/** "21 Eki" */
fun formatShortDate(date: LocalDate): String = date.format(shortDateFormat)

/** Dün / Bugün / Yarın, gelecek hafta içi gün adı, daha uzağı için tarih. */
fun dayLabel(date: LocalDate, today: LocalDate): String =
    when (val days = ChronoUnit.DAYS.between(today, date)) {
        -1L -> "Dün"
        0L -> "Bugün"
        1L -> "Yarın"
        else -> when {
            days in 2..6 -> date.format(weekdayFormat).capitalized()
            date.year == today.year -> date.format(dayMonthFormat)
            else -> date.format(dayMonthYearFormat)
        }
    }

/** "Bugün 14:00" */
fun dueLabel(dueAt: Long, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String {
    val due = dueAt.toLocalDateTime(zone)
    return "${dayLabel(due.toLocalDate(), today)} ${due.format(timeFormat)}"
}

/** "Pazartesi" */
fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, turkish).capitalized()

/** Her gün / Hafta içi / Hafta sonu / Her Salı / Pt, Ça, Cu */
fun formatRepeatDays(days: Int): String = when (days) {
    EVERY_DAY -> "Her gün"
    WEEKDAYS -> "Hafta içi"
    WEEKEND -> "Hafta sonu"
    else -> days.days().let { chosen ->
        if (chosen.size == 1) "Her ${dayName(chosen.single())}" else chosen.joinToString(", ") { weekdayInitials[it.ordinal] }
    }
}

/** "Hafta içi 09:00" */
fun formatRepeat(days: Int, time: LocalTime): String = "${formatRepeatDays(days)} ${formatTime(time)}"

/** The time alone for today, otherwise the day: what fits next to a title in a widget. */
fun compactDueLabel(dueAt: Long, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String {
    val due = dueAt.toLocalDateTime(zone)
    return if (due.toLocalDate() == today) due.format(timeFormat) else dayLabel(due.toLocalDate(), today)
}

/** "2 madde kaldı: Süt, Ekmek"; null when nothing is left. */
fun itemsLeftLabel(items: List<ChecklistItem>): String? =
    items.filterNot { it.isDone }.takeIf { it.isNotEmpty() }
        ?.let { left -> "${left.size} madde kaldı: ${left.joinToString(", ") { it.text }}" }

/** "2 sa 15 dk", "45 dk" */
fun formatDuration(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    val hours = minutes / 60
    return if (hours > 0) "$hours sa ${minutes % 60} dk" else "$minutes dk"
}
