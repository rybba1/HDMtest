package com.example.hdm.model

import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val message: String,
    var user: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val id: String = UUID.randomUUID().toString()
    val isSent = MutableStateFlow(false)

    // TEN KONSTRUKTOR JEST NIEZBĘDNY DLA FileLogService
    // Naprawia błąd "No parameter with name 'isSentInitial' found"
    constructor(
        message: String,
        user: String,
        timestamp: Long,
        isSentInitial: Boolean
    ) : this(message, user, timestamp) {
        isSent.value = isSentInitial
    }

    fun getFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}