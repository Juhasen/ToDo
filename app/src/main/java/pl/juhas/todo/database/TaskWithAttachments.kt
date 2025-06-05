package pl.juhas.todo.database

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Klasa pomocnicza reprezentująca zadanie wraz z jego załącznikami.
 */
data class TaskWithAttachments(
    @Embedded val task: Task,

    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val attachments: List<Attachment>
)
