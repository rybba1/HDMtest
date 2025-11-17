// Ścieżka: com/example/hdm/model/BhpDataModels.kt
package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model danych statystyk BHP użytkownika (używany przez API i ViewModel).
 * Dopasowany do odpowiedzi z GET /api/bhp/ranking.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BhpStatsUser(
    val accuracy: Float,
    val correctAnswers: Int,
    val login: String, // Używane do identyfikacji
    val name: String,
    val surname: String,
    val totalAnswers: Int,
    val totalPoints: Int,
    val wrongAnswers: Int
    // Usunięto: userId, lastAnswerTimestamp
) {
    // Pomocnicza właściwość do wyświetlania pełnej nazwy
    val fullName: String
        get() = "$name $surname"
}

/**
 * Model danych wysyłanych na serwer po zalogowaniu (POST /api/bhp/submit).
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SubmitPointsRequest(
    val userId: Int, // Nadal wymagane przez specyfikację POST /submit
    val login: String,
    val pointsToAdd: Int,
    val correctAnswersToAdd: Int,
    val wrongAnswersToAdd: Int
)

/**
 * Model odpowiedzi serwera po wysłaniu punktów (POST /api/bhp/submit).
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SubmitPointsResponse(
    val message: String,
    val success: Boolean,
    @SerialName("updated_data") // Dopasowanie do nazwy pola w JSON
    val updatedData: UpdatedData? = null // Opcjonalne, na wypadek błędu
)

/**
 * Część odpowiedzi SubmitPointsResponse.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdatedData(
    val accuracy: Float,
    val totalAnswers: Int,
    val totalPoints: Int
)


/**
 * Reprezentuje pojedynczy punkt oczekujący zapisany lokalnie.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PendingQuizPoints(
    val timestamp: Long,
    val quizId: Int,
    val wasCorrect: Boolean,
    val points: Int // +1 lub -1
)

/**
 * Struktura pomocnicza zwracana przez QuizStatisticsRepository.getAndClearPendingStats()
 */
data class PendingStats(
    val pointsToAdd: Int,
    val correctAnswersToAdd: Int,
    val wrongAnswersToAdd: Int
)