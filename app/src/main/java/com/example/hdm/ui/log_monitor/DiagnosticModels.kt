package com.example.hdm.ui.log_monitor

import kotlinx.serialization.SerialName

// === POCZĄTEK POPRAWKI: Przeniesiona klasa PingResult ===
data class PingResult(
    val rawResponse: String,
    val roundTripTimeMs: Long,
    val httpCode: Int
)
// === KONIEC POPRAWKI ===

// --- Modele dla Pingu ---
enum class PingStatus { SUCCESS, FAILED, TIMEOUT }

// --- Strategie Retry ---
enum class RetryStrategy {
    NONE,           // Bez retry
    SIMPLE,         // Prosty retry (3x ze stałym opóźnieniem)
    EXPONENTIAL,    // Exponential backoff
    INTELLIGENT     // Inteligentny retry z circuit breaker
}

// --- Konfiguracja Diagnostyczna ---
data class DiagnosticConfig(
    val retryStrategy: RetryStrategy = RetryStrategy.NONE,
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 500L,
    val connectionTimeout: Long = 10000L, // To pole na razie nie jest zaimplementowane w VM
    val useConnectionPooling: Boolean = false, // To pole na razie nie jest zaimplementowane w VM
    val throttleDelayMs: Long = 0L
)

data class PingLog(
    val id: Int,
    val timestamp: Long,
    val roundTripTimeMs: Long,
    val status: PingStatus,
    val message: String,
    val serverResponse: String?,
    val serverProcessingTimeMs: Double?,
    val clientIp: String?,
    val clientPort: String?,
    val ssid: String?,
    val rssi: Int?,
    val bssid: String?,
    val linkSpeed: Int?,
    val frequency: Int?,
    val downstreamBw: Int?,
    val upstreamBw: Int?,
    val retryAttempts: Int = 0, // Ile razy próbowano
    val totalTime: Long = roundTripTimeMs // Całkowity czas z retry
)

// --- Modele dla Zdarzeń Sieciowych ---
enum class NetworkEventType { AVAILABLE, LOST, CAPABILITIES_CHANGED }

data class NetworkEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: NetworkEventType,
    val message: String
)

// --- Wspólny interfejs dla połączonej listy ---
interface DiagnosticEvent {
    val timestamp: Long
}

data class PingEvent(val log: PingLog) : DiagnosticEvent {
    override val timestamp: Long = log.timestamp
}

data class NetworkStateEvent(val event: NetworkEvent) : DiagnosticEvent {
    override val timestamp: Long = event.timestamp
}

// --- Model statystyk ---
data class PingStatistics(
    val totalRequests: Int = 0,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val totalRetries: Int = 0,
    val averageResponseTime: Long = 0
) {
    val successRate: Int get() = if (totalRequests > 0) (successfulRequests * 100) / totalRequests else 0
    val failureRate: Int get() = if (totalRequests > 0) (failedRequests * 100) / totalRequests else 0
}