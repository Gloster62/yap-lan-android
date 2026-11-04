package com.gloster.yaplan.ui.edit

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gloster.yaplan.YapLanApp
import com.gloster.yaplan.data.Category
import com.gloster.yaplan.data.ChecklistItem
import com.gloster.yaplan.data.DEFAULT_REPEAT_MINUTES
import com.gloster.yaplan.data.Priority
import com.gloster.yaplan.data.Task
import com.gloster.yaplan.data.TaskRepository
import com.gloster.yaplan.data.repeatTime
import com.gloster.yaplan.data.timeOfDay
import com.gloster.yaplan.data.toggleDay
import com.gloster.yaplan.ui.toLocalDateTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditState(
    val isNew: Boolean = true,
    val loaded: Boolean = false,
    val title: String = "",
    val note: String = "",
    val isDone: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val categoryId: Long? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime = DEFAULT_TIME,
    val repeatDays: Int = 0,
    val repeatTime: LocalTime = timeOfDay(DEFAULT_REPEAT_MINUTES),
    val reminderAlarm: Boolean = false,
    /** Items not saved yet have negative ids, so each row still has its own key. */
    val items: List<ChecklistItem> = emptyList(),
    /** What is typed in "Madde ekle"; saving keeps it even if Enter wasn't pressed. */
    val newItemText: String = "",
    val createdAt: Long? = null,
) {
    val canSave: Boolean get() = title.isNotBlank()
    val hasDate: Boolean get() = dueDate != null
    val repeats: Boolean get() = repeatDays != 0

    /** A repeating task has no due date; [dueDate] is kept so clearing the days brings it back. */
    fun dueAt(zone: ZoneId): Long? =
        dueDate?.takeUnless { repeats }?.atTime(dueTime)?.atZone(zone)?.toInstant()?.toEpochMilli()

    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(18, 0)
    }
}

private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

@Stable
interface TaskFormActions {
    fun setTitle(value: String)
    fun setNote(value: String)
    fun setDone(value: Boolean)
    fun setPriority(value: Priority)
    fun setCategory(value: Long?)
    fun setDueDate(value: LocalDate?)
    fun setDueTime(value: LocalTime)
    fun toggleRepeatDay(day: DayOfWeek)
    fun setRepeatTime(value: LocalTime)
    fun setReminderAlarm(value: Boolean)
    fun setNewItemText(value: String)
    fun addNewItem()
    fun setItemText(id: Long, text: String)
    fun setItemDone(id: Long, done: Boolean)
    fun removeItem(id: Long)
    fun createCategory(category: Category)
}

class TaskEditViewModel(
    private val repository: TaskRepository,
    savedState: SavedStateHandle,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel(), TaskFormActions {
    val taskId: Long = savedState.get<Long>(ARG_TASK_ID) ?: NEW_TASK
    private var original: Task? = null
    private var nextDraftId = -1L

    private val _state = MutableStateFlow(newTaskState())
    val state: StateFlow<TaskEditState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> =
        repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (taskId != NEW_TASK) {
            viewModelScope.launch {
                val task = repository.getTask(taskId) ?: return@launch
                val items = repository.getItems(taskId)
                original = task
                val due = task.dueAt?.toLocalDateTime(zone)
                _state.value = TaskEditState(
                    isNew = false,
                    loaded = true,
                    title = task.title,
                    note = task.note,
                    isDone = task.isDone,
                    priority = task.priority,
                    categoryId = task.categoryId,
                    dueDate = due?.toLocalDate(),
                    dueTime = due?.toLocalTime()?.withSecond(0)?.withNano(0) ?: TaskEditState.DEFAULT_TIME,
                    repeatDays = task.repeatDays,
                    repeatTime = task.repeatTime,
                    reminderAlarm = task.reminderAlarm,
                    items = items,
                    createdAt = task.createdAt,
                )
                syncItemTicks()
            }
        }
    }

    /** A widget can tick items while this screen is open; saving must not undo that. */
    private suspend fun syncItemTicks() {
        repository.observeItems(taskId).collect { saved ->
            val doneById = saved.associate { it.id to it.isDone }
            _state.update { s ->
                s.copy(items = s.items.map { item -> doneById[item.id]?.let { item.copy(isDone = it) } ?: item })
            }
        }
    }

    private fun newTaskState() = TaskEditState(isNew = taskId == NEW_TASK, loaded = taskId == NEW_TASK)

    /** The "Yeni görev" sheet reuses one instance, so it starts clean each time it opens. */
    fun reset(title: String = "") {
        _state.value = newTaskState().copy(title = title)
    }

    override fun setTitle(value: String) = _state.update { it.copy(title = value) }
    override fun setNote(value: String) = _state.update { it.copy(note = value) }
    override fun setDone(value: Boolean) = _state.update { it.copy(isDone = value) }
    override fun setPriority(value: Priority) = _state.update { it.copy(priority = value) }
    override fun setCategory(value: Long?) = _state.update { it.copy(categoryId = value) }
    override fun setDueDate(value: LocalDate?) = _state.update { it.copy(dueDate = value) }
    override fun setDueTime(value: LocalTime) = _state.update { it.copy(dueTime = value) }
    override fun toggleRepeatDay(day: DayOfWeek) = _state.update { it.copy(repeatDays = it.repeatDays.toggleDay(day)) }
    override fun setRepeatTime(value: LocalTime) = _state.update { it.copy(repeatTime = value) }
    override fun setReminderAlarm(value: Boolean) = _state.update { it.copy(reminderAlarm = value) }

    /** A pasted multi-line list becomes one item per line. */
    override fun setNewItemText(value: String) {
        val lines = value.split('\n')
        _state.update { s ->
            s.copy(items = s.items + lines.dropLast(1).toDrafts(), newItemText = lines.last())
        }
    }

    override fun addNewItem() = _state.update { s ->
        s.copy(items = s.items + listOf(s.newItemText).toDrafts(), newItemText = "")
    }

    private fun List<String>.toDrafts(): List<ChecklistItem> =
        filter { it.isNotBlank() }.map { ChecklistItem(id = nextDraftId--, taskId = taskId, text = it.trim()) }

    override fun setItemText(id: Long, text: String) = updateItem(id) { it.copy(text = text) }

    /** Ticks on a saved task are kept right away, like ticking a task in the list. */
    override fun setItemDone(id: Long, done: Boolean) {
        updateItem(id) { it.copy(isDone = done) }
        if (id > 0) viewModelScope.launch { repository.setItemDone(id, done) }
    }

    override fun removeItem(id: Long) = _state.update { s -> s.copy(items = s.items.filterNot { it.id == id }) }

    private fun updateItem(id: Long, change: (ChecklistItem) -> ChecklistItem) =
        _state.update { s -> s.copy(items = s.items.map { if (it.id == id) change(it) else it }) }

    override fun createCategory(category: Category) {
        if (category.name.isBlank()) return
        viewModelScope.launch {
            val id = repository.saveCategory(category.copy(name = category.name.trim()))
            setCategory(id)
        }
    }

    fun save(onDone: () -> Unit) {
        addNewItem()
        val s = _state.value
        if (!s.canSave) return
        val base = original ?: Task(title = "")
        val task = base.copy(
            title = s.title.trim(),
            note = s.note.trim(),
            isDone = s.isDone,
            priority = s.priority,
            categoryId = s.categoryId,
            dueAt = s.dueAt(zone),
            repeatDays = s.repeatDays,
            repeatMinutes = s.repeatTime.minuteOfDay(),
            reminderAlarm = s.reminderAlarm,
        )
        val items = s.items
            .filter { it.text.isNotBlank() }
            .map { it.copy(id = it.id.coerceAtLeast(0), text = it.text.trim()) }
        viewModelScope.launch {
            repository.saveTask(task, items)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val task = original ?: return onDone()
        viewModelScope.launch {
            repository.deleteTask(task)
            onDone()
        }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
        const val NEW_TASK = -1L

        val Factory = viewModelFactory {
            initializer {
                TaskEditViewModel(
                    (this[APPLICATION_KEY] as YapLanApp).repository,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
