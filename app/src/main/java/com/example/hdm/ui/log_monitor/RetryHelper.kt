package com.example.hdm.ui.log_monitor

import android.util.Log
import kotlinx.coroutines.delay
import java.net.SocketTimeoutException

/**
 * Klasa pomocnicza do obsługi różnych strategii retry dla requestów sieciowych
 */
class RetryHelper(private val config: DiagnosticConfig) {

    private var circuitBreakerFailures = 0
    private var circuitBreakerLastFailure = 0L
    private val circuitBreakerResetTimeMs = 30000L // 30s

    /**
     * Wykonuje operację z wybraną strategią retry
     * @return Pair<Result, RetryAttempts>
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T
    ): Pair<Result<T>, Int> {
        return when (config.retryStrategy) {
            RetryStrategy.NONE -> {
                try {
                    Pair(Result.success(operation()), 0)
                } catch (e: Exception) {
                    Pair(Result.failure(e), 0)
                }
            }
            RetryStrategy.SIMPLE -> executeSimpleRetry(operation)
            RetryStrategy.EXPONENTIAL -> executeExponentialRetry(operation)
            RetryStrategy.INTELLIGENT -> executeIntelligentRetry(operation)
        }
    }

    /**
     * Prosta strategia - stałe opóźnienie między próbami
     */
    private suspend fun <T> executeSimpleRetry(
        operation: suspend () -> T
    ): Pair<Result<T>, Int> {
        var lastException: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                val result = operation()
                return Pair(Result.success(result), attempt)
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < config.maxRetries - 1) {
                    Log.d("RetryHelper", "SIMPLE retry ${attempt + 1}/${config.maxRetries}")
                    delay(config.baseDelayMs)
                }
            } catch (e: Exception) {
                return Pair(Result.failure(e), attempt)
            }
        }

        return Pair(Result.failure(lastException ?: Exception("Unknown error")), config.maxRetries)
    }

    /**
     * Exponential backoff - rosnące opóźnienie
     */
    private suspend fun <T> executeExponentialRetry(
        operation: suspend () -> T
    ): Pair<Result<T>, Int> {
        var lastException: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                val result = operation()
                return Pair(Result.success(result), attempt)
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < config.maxRetries - 1) {
                    val delayMs = config.baseDelayMs * (1 shl attempt) // 500ms, 1s, 2s, 4s...
                    Log.d("RetryHelper", "EXPONENTIAL retry ${attempt + 1}/${config.maxRetries}, delay: ${delayMs}ms")
                    delay(delayMs)
                }
            } catch (e: Exception) {
                return Pair(Result.failure(e), attempt)
            }
        }

        return Pair(Result.failure(lastException ?: Exception("Unknown error")), config.maxRetries)
    }

    /**
     * Inteligentna strategia z circuit breaker
     */
    private suspend fun <T> executeIntelligentRetry(
        operation: suspend () -> T
    ): Pair<Result<T>, Int> {
        // Sprawdź circuit breaker
        if (circuitBreakerFailures >= 5) {
            val timeSinceLastFailure = System.currentTimeMillis() - circuitBreakerLastFailure
            if (timeSinceLastFailure < circuitBreakerResetTimeMs) {
                Log.w("RetryHelper", "Circuit breaker OPEN - blokuję request")
                return Pair(
                    Result.failure(Exception("Circuit breaker OPEN - serwer tymczasowo niedostępny")),
                    0
                )
            } else {
                // Reset circuit breaker
                Log.i("RetryHelper", "Circuit breaker RESET - próbuję ponownie")
                circuitBreakerFailures = 0
            }
        }

        var lastException: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                val result = operation()
                // Sukces - zresetuj circuit breaker
                circuitBreakerFailures = 0
                return Pair(Result.success(result), attempt)
            } catch (e: SocketTimeoutException) {
                lastException = e
                circuitBreakerFailures++
                circuitBreakerLastFailure = System.currentTimeMillis()

                if (attempt < config.maxRetries - 1) {
                    // Inteligentne opóźnienie: dłuższe gdy więcej błędów
                    val baseDelay = config.baseDelayMs * (1 shl attempt)
                    val circuitBreakerMultiplier = 1 + (circuitBreakerFailures * 0.2f)
                    val delayMs = (baseDelay * circuitBreakerMultiplier).toLong()

                    Log.d("RetryHelper", "INTELLIGENT retry ${attempt + 1}/${config.maxRetries}, " +
                            "failures: $circuitBreakerFailures, delay: ${delayMs}ms")
                    delay(delayMs)
                }
            } catch (e: Exception) {
                return Pair(Result.failure(e), attempt)
            }
        }

        return Pair(Result.failure(lastException ?: Exception("Unknown error")), config.maxRetries)
    }

    /**
     * Resetuje stan circuit breakera (użyteczne przy zmianie konfiguracji)
     */
    fun resetCircuitBreaker() {
        circuitBreakerFailures = 0
        circuitBreakerLastFailure = 0L
        Log.i("RetryHelper", "Circuit breaker został zresetowany")
    }
}