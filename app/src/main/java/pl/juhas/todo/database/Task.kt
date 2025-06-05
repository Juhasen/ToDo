package pl.juhas.todo.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Task(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String?,
    val createdAt: Long,
    val status: TaskStatus = TaskStatus.TODO,
    val notify: Boolean = false,
    val notifyAt: Long? = null,
    val category: Category = Category.NORMAL,
)
