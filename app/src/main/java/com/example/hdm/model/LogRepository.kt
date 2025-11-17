// LogRepository.kt

package com.example.hdm.model

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object LogRepository {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    suspend fun initialize(context: Context) {
        val fileLogs = FileLogService.readLogs(context)
        // === POCZĄTEK ZMIANY ===
        // Sortujemy wczytaną listę malejąco po timestampie (od najnowszych)
        _logs.value = fileLogs.sortedByDescending { it.timestamp }
        // === KONIEC ZMIANY ===
    }

    fun addLog(logEntry: LogEntry) {
        _logs.update { currentLogs ->
            // Ta linia już poprawnie sortowała, więc zostaje bez zmian
            (listOf(logEntry) + currentLogs).sortedByDescending { it.timestamp }
        }
    }

    fun updateLogEntry(updatedLog: LogEntry) {
        _logs.update { currentLogs ->
            val index = currentLogs.indexOfFirst { it.id == updatedLog.id }
            if (index != -1) {
                val mutableList = currentLogs.toMutableList()
                mutableList[index] = updatedLog
                mutableList
            } else {
                currentLogs
            }
        }
    }
}