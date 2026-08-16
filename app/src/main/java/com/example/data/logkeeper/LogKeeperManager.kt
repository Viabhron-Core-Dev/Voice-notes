package com.example.data.logkeeper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class TimeFilter(val label: String, val hours: Long) {
    SIX_HOURS("6h", 6L),
    TWELVE_HOURS("12h", 12L),
    TWENTY_FOUR_HOURS("24h", 24L),
    ALL("All", Long.MAX_VALUE)
}

object LogKeeperManager {
    private val idCounter = AtomicLong(1L)
    private const val MAX_LOG_ENTRIES = 1000

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _isLoggingEnabled = MutableStateFlow(true)
    val isLoggingEnabled: StateFlow<Boolean> = _isLoggingEnabled.asStateFlow()

    init {
        log(LogTag.System, "LogKeeper initialized")
        log(LogTag.System, "WindowInsets configured (Edge-to-Edge active)")
        log(LogTag.Navigation, "Navigated to: main")
    }

    fun setLoggingEnabled(enabled: Boolean) {
        _isLoggingEnabled.value = enabled
        if (enabled) {
            log(LogTag.System, "Logging resumed by user")
        }
    }

    fun log(tag: LogTag, message: String, level: LogLevel = LogLevel.INFO) {
        if (!_isLoggingEnabled.value) return

        val now = System.currentTimeMillis()
        val entry = LogEntry(
            id = idCounter.getAndIncrement(),
            timestamp = now,
            formattedTime = timeFormat.format(Date(now)),
            tag = tag,
            message = message,
            level = level
        )

        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry) // Newest logs first
        if (currentList.size > MAX_LOG_ENTRIES) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun getFilteredLogs(filter: TimeFilter): List<LogEntry> {
        val allLogs = _logs.value
        if (filter == TimeFilter.ALL) return allLogs

        val cutoff = System.currentTimeMillis() - (filter.hours * 60 * 60 * 1000L)
        return allLogs.filter { it.timestamp >= cutoff }
    }

    fun formatLogsForExport(filter: TimeFilter): String {
        val filtered = getFilteredLogs(filter)
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("OFFLINE VOICE NOTES - LOG KEEPER AUDIT\n")
        sb.append("Exported: ").append(fullDateFormat.format(Date())).append("\n")
        sb.append("Filter Range: ").append(filter.label).append("\n")
        sb.append("Total Entries: ").append(filtered.size).append("\n")
        sb.append("========================================\n\n")

        for (entry in filtered.asReversed()) { // Chronological in export
            sb.append(fullDateFormat.format(Date(entry.timestamp)))
                .append(" [").append(entry.tag.displayName).append("] ")
                .append("[").append(entry.level.name).append("] ")
                .append(entry.message)
                .append("\n")
        }
        return sb.toString()
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log(LogTag.System, "Log buffer cleared")
    }
}
