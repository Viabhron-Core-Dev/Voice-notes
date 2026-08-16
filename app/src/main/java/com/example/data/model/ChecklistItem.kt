package com.example.data.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
) {
    companion object {
        fun parseFromContent(content: String): List<ChecklistItem> {
            if (content.isBlank()) return emptyList()
            return content.lines()
                .filter { it.isNotBlank() }
                .map { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("[x] ", ignoreCase = true) || trimmed.startsWith("[X] ") -> {
                            ChecklistItem(text = trimmed.substring(4), isChecked = true)
                        }
                        trimmed.startsWith("[ ] ") -> {
                            ChecklistItem(text = trimmed.substring(4), isChecked = false)
                        }
                        trimmed.startsWith("✓ ") -> {
                            ChecklistItem(text = trimmed.substring(2), isChecked = true)
                        }
                        else -> {
                            ChecklistItem(text = trimmed, isChecked = false)
                        }
                    }
                }
        }

        fun serializeToContent(items: List<ChecklistItem>): String {
            return items.joinToString("\n") { item ->
                val prefix = if (item.isChecked) "[x] " else "[ ] "
                "$prefix${item.text}"
            }
        }

        fun toPlainText(items: List<ChecklistItem>): String {
            return items.joinToString("\n") { it.text }
        }
    }
}
