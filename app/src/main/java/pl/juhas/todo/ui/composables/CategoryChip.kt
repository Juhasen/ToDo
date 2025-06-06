package pl.juhas.todo.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import pl.juhas.todo.database.Category

@Composable
fun CategoryChip(category: Category) {
    val categoryColors = mapOf(
        Category.NORMAL to MaterialTheme.colorScheme.primary,
        Category.WORK to MaterialTheme.colorScheme.tertiary,
        Category.PERSONAL to MaterialTheme.colorScheme.secondary,
        Category.SHOPPING to MaterialTheme.colorScheme.error,
        Category.URGENT to MaterialTheme.colorScheme.error,
        Category.OTHER to MaterialTheme.colorScheme.outline
    )

    val chipColor = categoryColors[category] ?: MaterialTheme.colorScheme.primary

    SuggestionChip(
        onClick = { },
        label = { Text(text = category.name) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = chipColor.copy(alpha = 0.2f),
            labelColor = chipColor
        )
    )
}