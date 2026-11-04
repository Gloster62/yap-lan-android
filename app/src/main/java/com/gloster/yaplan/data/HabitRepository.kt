package com.gloster.yaplan.data

import com.gloster.yaplan.reminder.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HabitRepository(
    db: AppDatabase,
    private val scheduler: ReminderScheduler,
    /** Refreshes the home-screen widgets; their sessions don't outlive a few seconds. */
    private val onChanged: suspend () -> Unit,
) {
    private val habits = db.habitDao()

    val allHabits: Flow<List<Habit>> = habits.observeAll()
    val allChecks: Flow<List<HabitCheck>> = habits.observeAllChecks()

    fun observeHabit(id: Long): Flow<Habit?> = habits.observe(id)

    fun observeDoneDays(id: Long): Flow<Set<LocalDate>> = habits.observeDays(id).map { it.toDates() }

    suspend fun getHabit(id: Long): Habit? = habits.get(id)

    suspend fun doneDays(id: Long): Set<LocalDate> = habits.days(id).toDates()

    suspend fun saveHabit(habit: Habit): Habit {
        val id = habits.upsert(habit).let { if (it == -1L) habit.id else it }
        val saved = habit.copy(id = id)
        scheduler.updateHabit(saved)
        onChanged()
        return saved
    }

    suspend fun deleteHabit(habit: Habit) {
        habits.delete(habit)
        scheduler.cancelHabit(habit.id)
        onChanged()
    }

    suspend fun setDone(habitId: Long, day: LocalDate, done: Boolean, today: LocalDate = LocalDate.now()) {
        if (!canToggle(day, today)) return
        val check = HabitCheck(habitId, day.toEpochDay())
        if (done) habits.insertCheck(check) else habits.deleteCheck(check)
        if (done && day == today) scheduler.stopHabitAlarm(habitId)
        onChanged()
    }

    suspend fun rescheduleReminders() {
        habits.withReminder().forEach(scheduler::updateHabit)
        scheduler.scheduleChainSaver()
    }

    /** Chains that break at midnight unless done today, longest first. */
    suspend fun atRiskToday(today: LocalDate = LocalDate.now()): List<AtRiskChain> {
        val doneByHabit = habits.allChecks().doneDaysByHabit()
        return habits.all()
            .mapNotNull { habit ->
                val done = doneByHabit[habit.id].orEmpty()
                if (isAtRisk(done, today)) AtRiskChain(habit, currentStreak(done, today)) else null
            }
            .sortedByDescending { it.streak }
    }
}

data class AtRiskChain(val habit: Habit, val streak: Int)

private fun List<Long>.toDates(): Set<LocalDate> = mapTo(HashSet(), LocalDate::ofEpochDay)

fun List<HabitCheck>.doneDaysByHabit(): Map<Long, Set<LocalDate>> =
    groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.day) }).mapValues { it.value.toSet() }
