package pl.juhas.todo.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<Attachment>)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM Attachment WHERE taskId = :taskId")
    suspend fun deleteAttachmentsForTask(taskId: Int)

    @Query("SELECT * FROM Attachment WHERE taskId = :taskId")
    suspend fun getAttachmentsForTask(taskId: Int): List<Attachment>

    @Transaction
    @Query("SELECT * FROM Task WHERE id = :taskId")
    suspend fun getTaskWithAttachments(taskId: Int): TaskWithAttachments

    @Transaction
    @Query("SELECT * FROM Task")
    suspend fun getAllTasksWithAttachments(): List<TaskWithAttachments>
}
