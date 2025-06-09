package pl.juhas.todo.database

import androidx.room.*

@Dao
interface TaskDao {
    @Query("SELECT * FROM Task ORDER BY finishAt ASC")
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
