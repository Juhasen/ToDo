package pl.juhas.todo.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.juhas.todo.database.Attachment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentItem(
    attachment: Attachment,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val isImage = attachment.mimeType.startsWith("image/")

    // Określenie koloru ikony na podstawie typu pliku
    val iconTint = when {
        isImage -> MaterialTheme.colorScheme.tertiary
        attachment.mimeType.contains("pdf") -> MaterialTheme.colorScheme.error
        attachment.mimeType.contains("doc") -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = onOpen,
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
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isImage) Icons.Default.Image else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = "Typ załącznika",
                modifier = Modifier.size(40.dp),
                tint = iconTint
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            FilledIconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń załącznik",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

