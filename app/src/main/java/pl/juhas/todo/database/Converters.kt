package pl.juhas.todo.database

import androidx.room.TypeConverter

/**
 * Konwertery dla typów używanych w bazie danych Room
 */
class Converters {
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(status: String): TaskStatus {
        return try {
            TaskStatus.valueOf(status)
        } catch (e: Exception) {
            TaskStatus.TODO
        }
    }

    @TypeConverter
    fun fromCategory(category: Category): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(category: String): Category {
        return try {
            Category.valueOf(category)
        } catch (e: Exception) {
            Category.NORMAL
        }
    }
}
