// Ścieżka: com/example/hdm/repository/BhpStatsRepository.kt
package com.example.hdm.repository

import com.example.hdm.model.BhpStatsUser
import com.example.hdm.model.SubmitPointsRequest
import com.example.hdm.model.SubmitPointsResponse
import com.example.hdm.services.BhpApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BhpStatsRepository @Inject constructor(
    private val bhpApiService: BhpApiService
) {

    /**
     * Wysyła zebrane punkty na serwer.
     * Zwraca teraz SubmitPointsResponse.
     */
    suspend fun submitPoints(
        userId: Int,
        login: String,
        pointsToAdd: Int,
        correctAnswersToAdd: Int,
        wrongAnswersToAdd: Int
    ): Result<SubmitPointsResponse> { // Zmieniono typ zwracany
        val request = SubmitPointsRequest(
            userId = userId,
            login = login,
            pointsToAdd = pointsToAdd,
            correctAnswersToAdd = correctAnswersToAdd,
            wrongAnswersToAdd = wrongAnswersToAdd
        )
        return bhpApiService.submitPoints(request)
    }

    /**
     * Pobiera pełną listę statystyk wszystkich użytkowników z serwera.
     */
    suspend fun getAllStats(): Result<List<BhpStatsUser>> {
        return bhpApiService.getAllStats()
    }
}