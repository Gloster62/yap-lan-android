package com.gloster.yaplan.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

enum class Priority(val label: String) {
    HIGH("Yüksek"),
    MEDIUM("Orta"),
    LOW("Düşük"),
}

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val isDone: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val categoryId: Long? = null,
    /** Son tarih, epoch milisaniye; o saatte bildirim gelir. */
    val dueAt: Long? = null,
    /** Düzenli hatırlatma günleri (bkz. [hasDay]); 0 ise kapalı. */
    val repeatDays: Int = 0,
    /** Düzenli hatırlatmanın saati, günün dakikası (09:00 → 540). */
    val repeatMinutes: Int = DEFAULT_REPEAT_MINUTES,
    /** Hatırlatma bildirim yerine tam ekran alarm olarak çalar. */
    @ColumnInfo(defaultValue = "0") val reminderAlarm: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

const val DEFAULT_REPEAT_MINUTES = 9 * 60

val Task.repeatTime: LocalTime get() = timeOfDay(repeatMinutes)
