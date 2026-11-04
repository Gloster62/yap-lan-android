package com.gloster.yaplan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Task::class, Category::class, Habit::class, HabitCheck::class, ChecklistItem::class],
    version = 5,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun habitDao(): HabitDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        private val defaultCategories = listOf("İş", "Ev", "Okul", "Kişisel")

        /** Copied from schemas/…/2.json so Room's schema check accepts the migrated tables. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `reminderMinutes` INTEGER, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit_checks` (`habitId` INTEGER NOT NULL, `day` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`habitId`, `day`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
            }
        }

        /**
         * Drops `remind` (every dated task reminds now) and adds weekly reminders and lists.
         * SQLite before Android 14 can't drop a column, so the table is copied; SQL from schemas/…/3.json.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `note` TEXT NOT NULL, `isDone` INTEGER NOT NULL, `priority` TEXT NOT NULL, " +
                        "`categoryId` INTEGER, `dueAt` INTEGER, `repeatDays` INTEGER NOT NULL, " +
                        "`repeatMinutes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) " +
                        "REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                )
                db.execSQL(
                    "INSERT INTO `tasks_new` (`id`, `title`, `note`, `isDone`, `priority`, `categoryId`, `dueAt`, " +
                        "`repeatDays`, `repeatMinutes`, `createdAt`) SELECT `id`, `title`, `note`, `isDone`, `priority`, " +
                        "`categoryId`, `dueAt`, 0, $DEFAULT_REPEAT_MINUTES, `createdAt` FROM `tasks`",
                )
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `checklist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, `text` TEXT NOT NULL, `isDone` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_taskId` ON `checklist_items` (`taskId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderAlarm` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderAlarm` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "yaplan.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        defaultCategories.forEachIndexed { i, name ->
                            db.execSQL(
                                "INSERT INTO categories (name, color) VALUES (?, ?)",
                                arrayOf<Any>(name, CategoryColors[i]),
                            )
                        }
                    }
                })
                .build()
    }
}
