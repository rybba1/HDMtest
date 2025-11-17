// FileLogService.kt

package com.example.hdm.model

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@SuppressLint("UnsafeOptInUsageError")
@Serializable
private data class SerializableLogEntry(
    val message: String,
    val user: String,
    val timestamp: Long,
    val isSent: Boolean
)

object FileLogService {
    private const val LOG_FILE_NAME = "hdm_app_logs.json"
    private const val TAG = "FileLogService"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun readLogs(context: Context): List<LogEntry> = withContext(Dispatchers.IO) {
        val logFile = File(context.filesDir, LOG_FILE_NAME)

        if (!logFile.exists()) {
            return@withContext emptyList()
        }
        return@withContext try {
            val fileContent = logFile.readText()
            if (fileContent.isBlank()) emptyList()
            else {
                val serializableLogs = json.decodeFromString<List<SerializableLogEntry>>(fileContent)
                serializableLogs.map { LogEntry(it.message, it.user, it.timestamp, it.isSent) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd odczytu logów z pliku.", e)
            emptyList()
        }
    }

    suspend fun appendLog(context: Context, logEntry: LogEntry) {
        try {
            val currentLogs = readLogs(context).toMutableList()
            currentLogs.add(logEntry)
            updateLogs(context, currentLogs)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd dopisywania logu do pliku.", e)
        }
    }

    suspend fun updateLogs(context: Context, logs: List<LogEntry>) = withContext(Dispatchers.IO) {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        try {
            val serializableLogs = logs.map {
                SerializableLogEntry(it.message, it.user, it.timestamp, it.isSent.value)
            }
            val jsonString = json.encodeToString(serializableLogs)
            logFile.writeText(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd zapisu logów do pliku.", e)
        }
    }
}