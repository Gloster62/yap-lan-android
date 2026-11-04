package com.gloster.yaplan.ui.edit

import com.gloster.yaplan.data.toggleDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskEditStateTest {
    private val date = LocalDate.of(2026, 11, 5)
    private val dated = TaskEditState(dueDate = date, dueTime = LocalTime.of(18, 0))
    private val monday = 0.toggleDay(DayOfWeek.MONDAY)

    @Test
    fun datedTaskIsDueAtItsTime() {
        val expected = date.atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, dated.dueAt(ZoneOffset.UTC))
    }

    @Test
    fun repeatingTaskHasNoDueDate() {
        assertNull(dated.copy(repeatDays = monday).dueAt(ZoneOffset.UTC))
    }

    @Test
    fun clearingTheDaysBringsTheDateBack() {
        val repeating = dated.copy(repeatDays = monday)
        assertEquals(dated.dueAt(ZoneOffset.UTC), repeating.copy(repeatDays = 0).dueAt(ZoneOffset.UTC))
    }
}
