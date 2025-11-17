// Ścieżka: com/example/hdm/ui/bhpstats/BhpStatsViewModel.kt
package com.example.hdm.ui.bhpstats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.model.BhpStatsUser
import com.example.hdm.model.UserManager
import com.example.hdm.repository.BhpStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BhpStatsUiState(
    val isLoading: Boolean = true,
    val currentUserStats: BhpStatsUser? = null, // Statystyki zalogowanego użytkownika
    val ranking: List<BhpStatsUser> = emptyList(), // Top 10 posortowane po stronie klienta
    val userPositionInRanking: Int = 0, // 0 oznacza brak w rankingu (pełnym)
    val error: String? = null
)

@HiltViewModel
class BhpStatsViewModel @Inject constructor(
    private val bhpStatsRepository: BhpStatsRepository,
    private val userManager: UserManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BhpStatsUiState())
    val uiState: StateFlow<BhpStatsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUserLogin = userManager.loggedInUser.value?.login // Pobierz login zalogowanego

            if (currentUserLogin == null) {
                _uiState.update { it.copy(isLoading = false, error = "Nie jesteś zalogowany.") }
                return@launch
            }

            val result = bhpStatsRepository.getAllStats() // Pobierz WSZYSTKIE statystyki

            result.onSuccess { allStats ->
                // Znajdź statystyki bieżącego użytkownika na liście po login
                val currentUserStats = allStats.find { it.login == currentUserLogin }

                // Posortuj listę malejąco po punktach, aby uzyskać ranking
                val sortedRanking = allStats.sortedByDescending { it.totalPoints }

                // Znajdź pozycję użytkownika w posortowanym rankingu po login
                val position = sortedRanking.indexOfFirst { it.login == currentUserLogin } + 1 // +1 bo indeksy są od 0

                // Weź tylko Top 10 do wyświetlenia
                val top10Ranking = sortedRanking.take(10)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserStats = currentUserStats,
                        ranking = top10Ranking, // Wyświetlamy tylko top 10
                        userPositionInRanking = if(position > 0) position else 0, // Pełna pozycja w rankingu
                        error = null
                    )
                }
            }.onFailure { e ->
                Log.e("BhpStatsVM", "Błąd podczas pobierania wszystkich statystyk", e)
                _uiState.update { it.copy(isLoading = false, error = "Błąd pobierania danych: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}