package pl.juhas.todo.database

import androidx.room.*

@Dao
interface TaskDao {
    @Query("SELECT * FROM Task ORDER BY " +
           "CASE WHEN status = 'TODO' AND finishAt IS NOT NULL THEN 0 ELSE 1 END, " +
           "CASE WHEN finishAt IS NOT NULL THEN finishAt ELSE 9223372036854775807 END, " +
           "createdAt DESC")
    suspend fun getAllTasks(): List<Task>

    @Query("SELECT * FROM Task WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM Task WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)
}
