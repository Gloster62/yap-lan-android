package com.gloster.yaplan.ui

import com.gloster.yaplan.data.ChecklistItem
import com.gloster.yaplan.data.EVERY_DAY
import com.gloster.yaplan.data.WEEKDAYS
import com.gloster.yaplan.data.WEEKEND
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {
    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun dayLabelsUsePlainTurkish() {
        assertEquals("Dün", dayLabel(today.minusDays(1), today))
        assertEquals("Bugün", dayLabel(today, today))
        assertEquals("Yarın", dayLabel(today.plusDays(1), today))
        assertEquals("Perşembe", dayLabel(today.plusDays(3), today))
        assertEquals("2 Kasım", dayLabel(LocalDate.of(2026, 11, 2), today))
        assertEquals("5 Ocak 2027", dayLabel(LocalDate.of(2027, 1, 5), today))
    }

    @Test
    fun dueLabelCombinesDayAndTime() {
        val dueAt = LocalDateTime.of(2026, 10, 18, 18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("Dün 18:00", dueLabel(dueAt, today, ZoneOffset.UTC))
    }

    @Test
    fun headerDate() {
        assertEquals("Pazartesi, 19 Ekim", formatHeaderDate(today))
    }

    @Test
    fun monthTitle() {
        assertEquals("Ekim 2026", formatMonth(YearMonth.of(2026, 10)))
        assertEquals("Şubat 2027", formatMonth(YearMonth.of(2027, 2)))
    }

    @Test
    fun repeatDaysReadNaturally() {
        val mondayWednesdayFriday = 0b001_0101
        assertEquals("Her gün 09:00", formatRepeat(EVERY_DAY, LocalTime.of(9, 0)))
        assertEquals("Hafta içi", formatRepeatDays(WEEKDAYS))
        assertEquals("Hafta sonu", formatRepeatDays(WEEKEND))
        assertEquals("Her Salı", formatRepeatDays(0b000_0010))
        assertEquals("Pt, Ça, Cu", formatRepeatDays(mondayWednesdayFriday))
    }

    @Test
    fun compactDueLabelShowsOnlyTheTimeForToday() {
        fun at(day: Int, hour: Int) = LocalDateTime.of(2026, 10, day, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("18:00", compactDueLabel(at(19, 18), today, ZoneOffset.UTC))
        assertEquals("Yarın", compactDueLabel(at(20, 9), today, ZoneOffset.UTC))
        assertEquals("Dün", compactDueLabel(at(18, 9), today, ZoneOffset.UTC))
    }

    @Test
    fun itemsLeftNamesOnlyWhatIsNotDone() {
        val milk = ChecklistItem(taskId = 1, text = "Süt")
        val bread = ChecklistItem(taskId = 1, text = "Ekmek")
        val eggs = ChecklistItem(taskId = 1, text = "Yumurta", isDone = true)
        assertEquals("2 madde kaldı: Süt, Ekmek", itemsLeftLabel(listOf(milk, eggs, bread)))
        assertNull(itemsLeftLabel(listOf(eggs)))
        assertNull(itemsLeftLabel(emptyList()))
    }

    @Test
    fun durationsInHoursAndMinutes() {
        assertEquals("2 sa 15 dk", formatDuration((2 * 60 + 15) * 60_000L))
        assertEquals("45 dk", formatDuration(45 * 60_000L + 59_000))
        assertEquals("0 dk", formatDuration(-1))
    }
}
