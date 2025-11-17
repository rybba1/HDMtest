// Ścieżka: com/example/hdm/repository/QuizStatisticsRepository.kt
package com.example.hdm.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.hdm.model.PendingQuizPoints
import com.example.hdm.model.PendingStats // Zakładamy, że ten model jest zdefiniowany
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// QuizAnswerStats pozostaje bez zmian
data class QuizAnswerStats(
    val quizId: Int,
    val totalAnswers: Int,
    val answerCounts: List<Int> // liczba odpowiedzi dla każdej opcji [A, B, C]
) {
    fun getPercentages(): List<Float> {
        if (totalAnswers == 0) return List(answerCounts.size) { 0f }
        return answerCounts.map { (it.toFloat() / totalAnswers) * 100f }
    }
}

// PendingQuizPoints pozostaje bez zmian (przeniesione do BhpDataModels.kt)

object QuizStatisticsRepository {
    private const val PREFS_NAME = "quiz_statistics"
    private const val KEY_STATS = "stats" // Ogólne statystyki quizów
    private const val KEY_PENDING_POINTS = "pending_points" // Punkty przed logowaniem

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Sekcja ogólnych statystyk quizów (pozostaje) ---

    private fun loadAllQuizStats(context: Context): MutableMap<Int, QuizAnswerStats> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_STATS, null) ?: return mutableMapOf()

        return try {
            val type = object : TypeToken<MutableMap<Int, QuizAnswerStats>>() {}.type
            Gson().fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            Log.e("QuizStatsRepo", "Błąd odczytu statystyk quizów", e)
            mutableMapOf()
        }
    }

    private fun saveAllQuizStats(context: Context, stats: Map<Int, QuizAnswerStats>) {
        val prefs = getPrefs(context)
        val json = Gson().toJson(stats)
        prefs.edit().putString(KEY_STATS, json).apply()
    }

    /**
     * Zapisuje odpowiedź do OGÓLNYCH statystyk quizu (ile osób wybrało daną opcję).
     */
    fun recordAnswer(context: Context, quizId: Int, answerIndex: Int, totalAnswersInQuiz: Int = 3) {
        val allStats = loadAllQuizStats(context)

        val currentStats = allStats[quizId] ?: QuizAnswerStats(
            quizId = quizId,
            totalAnswers = 0,
            answerCounts = List(totalAnswersInQuiz) { 0 }
        )

        // Upewnij się, że lista ma odpowiedni rozmiar (na wypadek starych danych)
        val currentAnswerCounts = currentStats.answerCounts.toMutableList()
        while (currentAnswerCounts.size < totalAnswersInQuiz) {
            currentAnswerCounts.add(0)
        }

        // Sprawdź, czy indeks jest prawidłowy
        if (answerIndex >= 0 && answerIndex < currentAnswerCounts.size) {
            val newAnswerCounts = currentAnswerCounts.also {
                it[answerIndex] = it[answerIndex] + 1
            }
            val newStats = currentStats.copy(
                totalAnswers = currentStats.totalAnswers + 1,
                answerCounts = newAnswerCounts
            )
            allStats[quizId] = newStats
            saveAllQuizStats(context, allStats)
        } else {
            Log.e("QuizStatsRepo", "Nieprawidłowy answerIndex ($answerIndex) dla quizu $quizId z ${currentAnswerCounts.size} opcjami.")
        }
    }


    /**
     * Pobiera OGÓLNE statystyki dla danego pytania.
     */
    fun getStats(context: Context, quizId: Int): QuizAnswerStats? {
        return loadAllQuizStats(context)[quizId]
    }

    // --- Sekcja punktów oczekujących (pozostaje i jest rozbudowana) ---

    private fun loadPendingPoints(context: Context): MutableList<PendingQuizPoints> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_PENDING_POINTS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<PendingQuizPoints>>() {}.type
                Gson().fromJson<MutableList<PendingQuizPoints>>(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e("QuizStatsRepo", "Błąd odczytu punktów oczekujących", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    private fun savePendingPoints(context: Context, pendingList: List<PendingQuizPoints>) {
        val prefs = getPrefs(context)
        val newJson = Gson().toJson(pendingList)
        prefs.edit().putString(KEY_PENDING_POINTS, newJson).apply()
    }

    /**
     * Zapisuje oczekujące punkty (przed logowaniem).
     */
    fun addPendingPoints(context: Context, quizId: Int, wasCorrect: Boolean) {
        val pendingList = loadPendingPoints(context)
        val points = if (wasCorrect) 1 else -1
        pendingList.add(
            PendingQuizPoints(
                timestamp = System.currentTimeMillis(),
                quizId = quizId,
                wasCorrect = wasCorrect,
                points = points
            )
        )
        savePendingPoints(context, pendingList)
        Log.d("QuizStatsRepo", "Dodano punkt oczekujący: quizId=$quizId, correct=$wasCorrect, points=$points. Łącznie: ${pendingList.size}")
    }

    /**
     * Pobiera zagregowane statystyki oczekujące i CZYŚCI je z pamięci.
     * Używane przez LoginViewModel do wysłania na serwer.
     * @return PendingStats lub null, jeśli nie ma nic do wysłania.
     */
    fun getAndClearPendingStats(context: Context): PendingStats? {
        val pendingList = loadPendingPoints(context)
        if (pendingList.isEmpty()) {
            return null
        }

        val totalPoints = pendingList.sumOf { it.points }
        val correctCount = pendingList.count { it.wasCorrect }
        val wrongCount = pendingList.size - correctCount // Szybsze niż count { !it.wasCorrect }

        // Wyczyść punkty oczekujące
        clearPendingPoints(context)
        Log.d("QuizStatsRepo", "Pobrano i wyczyszczono ${pendingList.size} punktów oczekujących.")

        return PendingStats(
            pointsToAdd = totalPoints,
            correctAnswersToAdd = correctCount,
            wrongAnswersToAdd = wrongCount
        )
    }


    /**
     * Pobiera SUMĘ oczekujących punktów (bez czyszczenia).
     * Używane do pokazania badge'a na ekranie logowania.
     */
    fun getPendingPointsCount(context: Context): Int {
        val pendingList = loadPendingPoints(context)
        return pendingList.sumOf { it.points }
    }

    /**
     * Czyści tylko oczekujące punkty.
     */
    fun clearPendingPoints(context: Context) {
        getPrefs(context).edit().remove(KEY_PENDING_POINTS).apply()
        Log.d("QuizStatsRepo", "Wyczyszczono punkty oczekujące.")
    }

    // --- Funkcje usunięte (przeniesione na serwer i do BhpStatsRepository) ---
    // fun assignPendingPointsToUser(...)
    // fun getUserStats(...)
    // fun saveUserStats(...)
    // fun getAllUsersRanking(...)
    // fun getTopUsers(...)
    // fun hasUserStats(...)
    // fun getUserRankPosition(...)

    // --- Pozostałe funkcje pomocnicze/deweloperskie ---

    /**
     * Czyści WSZYSTKIE statystyki (quizów i oczekujące).
     * UWAGA: To usuwa dane!
     */
    fun clearAllStats(context: Context) {
        getPrefs(context).edit().clear().apply()
        Log.w("QuizStatsRepo", "Wyczyszczono WSZYSTKIE statystyki BHP (quizy i oczekujące).")
    }

    /**
     * Pobiera całkowitą liczbę odpowiedzi we wszystkich quizach
     * (suma wszystkich odpowiedzi od wszystkich użytkowników).
     */
    fun getTotalAnswersCountOverall(context: Context): Int {
        return loadAllQuizStats(context).values.sumOf { it.totalAnswers }
    }
}