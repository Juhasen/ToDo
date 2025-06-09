package pl.juhas.todo.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.juhas.todo.database.Attachment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAttachmentItem(
    attachment: Attachment,
    onClick: () -> Unit
) {
    val isImage = attachment.mimeType.startsWith("image/")

    // Określenie koloru ikony na podstawie typu pliku
    val iconTint = when {
        isImage -> MaterialTheme.colorScheme.tertiary
        attachment.mimeType.contains("pdf") -> MaterialTheme.colorScheme.error
        attachment.mimeType.contains("doc") -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isImage) Icons.Default.Image else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = "Typ załącznika",
                modifier = Modifier.size(40.dp),
                tint = iconTint
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Dodanie informacji o typie pliku
            Text(
                text = attachment.mimeType.split("/").lastOrNull() ?: "unknown",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}