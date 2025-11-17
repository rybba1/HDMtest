// Ścieżka: com/example/hdm/ui/login/LoginViewModel.kt
package com.example.hdm.ui.login

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.Screen
import com.example.hdm.model.UserManager
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.LogLevel
import com.example.hdm.services.LoginApiService
import com.example.hdm.services.LoginRepository
import com.example.hdm.services.UpdateManager
import com.example.hdm.repository.BhpStatsRepository
import com.example.hdm.repository.QuizStatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

// LoginState pozostaje bez zmian
sealed class LoginState {
    object Idle : LoginState()
    object Syncing : LoginState()
    object Scanning : LoginState()
    object Validating : LoginState()
    data class Success(val userName: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

// ===== POCZĄTEK ZMIANY 1 (Usunięcie pola manualPassword) =====
data class LoginUiState(
    val loginState: LoginState = LoginState.Idle,
    val isSyncInProgress: Boolean = false,
    val showCreateUserPasswordDialog: Boolean = false,
    val createUserPasswordError: Boolean = false
    // USUNIĘTO: manualPassword: String = ""
)
// ===== KONIEC ZMIANY 1 =====


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val application: Application,
    private val loginApiService: LoginApiService,
    private val hdmLogger: HdmLogger,
    private val updateManager: UpdateManager,
    private val quizStatisticsRepository: QuizStatisticsRepository,
    private val bhpStatsRepository: BhpStatsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val updateState = updateManager.updateState

    private val _navigationEvent = Channel<String>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private companion object {
        const val CORRECT_PASSWORD_1 = "Logistyka2025!"
        const val CORRECT_PASSWORD_2 = "adi123"
    }

    // ===== POCZĄTEK ZMIANY 2 (Usunięto funkcje do logowania ręcznego) =====
    // USUNIĘTO: fun onManualPasswordChange(password: String)
    // USUNIĘTO: fun loginWithManualPassword(context: Context)
    // ===== KONIEC ZMIANY 2 =====

    // Funkcje związane z aktualizacją
    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }
    fun startUpdateDownload(filename: String) {
        updateManager.startDownload(filename)
    }
    fun resetUpdateState() {
        updateManager.resetState()
    }

    // Funkcja synchronizacji poświadczeń
    fun syncCredentialsIfNeeded(context: Context) {
        viewModelScope.launch {
            val needsSync = LoginRepository.needsSync(context)

            if (needsSync) {
                _uiState.update { it.copy(loginState = LoginState.Syncing, isSyncInProgress = true) }
                hdmLogger.log(
                    context,
                    logMessage = "Rozpoczęto synchronizację bazy użytkowników z serwerem"
                )
                val result = LoginRepository.syncWithServer(context, loginApiService)
                result.onSuccess {
                    hdmLogger.log(
                        context,
                        logMessage = "Synchronizacja zakończona pomyślnie"
                    )
                }.onFailure { exception ->
                    hdmLogger.log(
                        context,
                        logMessage = "Błąd synchronizacji: ${exception.message}",
                        level = LogLevel.ERROR
                    )
                }
                _uiState.update { it.copy(loginState = LoginState.Idle, isSyncInProgress = false) }
            }
        }
    }


    // Funkcja logowania z obsługą wysyłki punktów
    fun loginWithCard(context: Context, scannedPassword: String) {
        if (_uiState.value.loginState !is LoginState.Idle) {
            Log.w("LoginViewModel", "Logowanie już w toku. Ignoruję kolejne wywołanie.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loginState = LoginState.Validating) }

            hdmLogger.log(
                context,
                logMessage = "Próba logowania przez kartę QR"
            )

            val credentials = LoginRepository.validateLogin(context, scannedPassword)

            if (credentials != null) {
                // 1. Zaloguj użytkownika lokalnie
                UserManager.loginUser(
                    login = credentials.login,
                    fullName = credentials.fullName,
                    workerId = credentials.workerId
                )
                hdmLogger.log(
                    context,
                    logMessage = "Zalogowano użytkownika: ${credentials.fullName} (${credentials.login}, ID: ${credentials.workerId})"
                )

                // 2. Pobierz i wyczyść punkty oczekujące
                val pendingStats = quizStatisticsRepository.getAndClearPendingStats(context)

                // 3. Jeśli są punkty, wyślij je na serwer w tle
                if (pendingStats != null && (pendingStats.correctAnswersToAdd > 0 || pendingStats.wrongAnswersToAdd > 0)) {
                    launch(Dispatchers.IO) { // Uruchom w tle
                        Log.d("LoginViewModel", "Wysyłanie punktów oczekujących na serwer: $pendingStats")
                        val submitResult = bhpStatsRepository.submitPoints(
                            userId = credentials.workerId,
                            login = credentials.login, // Dodano brakujący parametr
                            pointsToAdd = pendingStats.pointsToAdd,
                            correctAnswersToAdd = pendingStats.correctAnswersToAdd,
                            wrongAnswersToAdd = pendingStats.wrongAnswersToAdd
                        )
                        submitResult.onSuccess { response ->
                            Log.i("LoginViewModel", "Punkty BHP (${pendingStats.pointsToAdd}) pomyślnie wysłane dla user ${credentials.workerId}. Serwer: ${response.message}")
                        }.onFailure { e ->
                            Log.e("LoginViewModel", "Błąd wysyłania punktów BHP dla user ${credentials.workerId}", e)
                            hdmLogger.log(
                                context,
                                logMessage = "Nie udało się wysłać punktów BHP (${pendingStats.pointsToAdd}) na serwer dla ${credentials.fullName}. Błąd: ${e.message}",
                                level = LogLevel.ERROR
                            )
                        }
                    }
                } else {
                    Log.d("LoginViewModel", "Brak punktów oczekujących do wysłania.")
                }

                // 4. Zaktualizuj UI
                // ===== POCZĄTEK ZMIANY 3 (Czyszczenie hasła po sukcesie - usunięte) =====
                _uiState.update {
                    it.copy(loginState = LoginState.Success(credentials.fullName))
                }
                // ===== KONIEC ZMIANY 3 =====
            } else {
                // Obsługa błędu logowania
                hdmLogger.log(
                    context,
                    logMessage = "Nieprawidłowy kod QR - brak dopasowania w bazie",
                    level = LogLevel.WARNING
                )
                _uiState.update {
                    it.copy(
                        loginState = LoginState.Error(
                            "Nieprawidłowy kod QR.\nSprawdź czy baza użytkowników jest aktualna."
                        )
                    )
                }
            }
        }
    }


    // Funkcje resetowania stanu logowania i obsługi dodawania użytkownika
    fun resetLoginState() {
        _uiState.update { it.copy(loginState = LoginState.Idle) }
    }
    fun onAddNewUserClicked() {
        _uiState.update { it.copy(showCreateUserPasswordDialog = true) }
    }
    fun onPasswordForCreateUserConfirm(password: String, context: Context) {
        if (password == CORRECT_PASSWORD_1 || password == CORRECT_PASSWORD_2) {
            hdmLogger.log(context, "Autoryzowano dostęp do tworzenia użytkownika.")
            viewModelScope.launch {
                _navigationEvent.send(Screen.CreateUser.route)
            }
            dismissCreateUserPasswordDialog()
        } else {
            _uiState.update { it.copy(createUserPasswordError = true) }
        }
    }
    fun dismissCreateUserPasswordDialog() {
        _uiState.update { it.copy(showCreateUserPasswordDialog = false, createUserPasswordError = false) }
    }
}