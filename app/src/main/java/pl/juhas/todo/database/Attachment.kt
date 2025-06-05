package pl.juhas.todo.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int, // ID zadania, do którego należy załącznik
    val fileName: String, // Nazwa pliku
    val filePath: String, // Ścieżka do pliku w pamięci urządzenia lub URI
    val mimeType: String, // Typ MIME załącznika
    val fileSize: Long, // Rozmiar pliku w bajtach
    val createdAt: Long // Data utworzenia załącznika
)
