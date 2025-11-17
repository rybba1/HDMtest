package com.example.hdm.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.LogLevel
import com.example.hdm.services.LoginApiService
import com.example.hdm.services.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateUserUiState(
    val login: String = "",
    val password: String = "",
    val creationState: CreateUserState = CreateUserState.Idle
)

sealed class CreateUserState {
    object Idle : CreateUserState()
    object Sending : CreateUserState()
    data class Success(val message: String) : CreateUserState()
    data class Error(val message: String) : CreateUserState()
}

@HiltViewModel
class CreateUserViewModel @Inject constructor(
    private val loginApiService: LoginApiService,
    private val hdmLogger: HdmLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUserUiState())
    val uiState: StateFlow<CreateUserUiState> = _uiState.asStateFlow()

    fun setLogin(login: String) {
        _uiState.update { it.copy(login = login.trim()) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password.trim()) }
    }

    fun createUser(context: Context) {
        val currentState = _uiState.value

        if (currentState.login.isBlank() || currentState.password.isBlank()) {
            return
        }

        viewModelScope.launch {
            hdmLogger.log(
                context,
                logMessage = "Rozpoczęto tworzenie użytkownika: login='${currentState.login}'"
            )

            _uiState.update { it.copy(creationState = CreateUserState.Sending) }

            val result = loginApiService.createUser(
                login = currentState.login,
                password = currentState.password
            )

            result.onSuccess { response ->
                hdmLogger.log(
                    context,
                    logMessage = "Pomyślnie utworzono użytkownika: ${response.getDisplayMessage()}",
                    level = LogLevel.INFO // <-- POPRAWKA
                )
                _uiState.update {
                    it.copy(
                        creationState = CreateUserState.Success(
                            message = response.getDisplayMessage()
                        )
                    )
                }

                // --- POCZĄTEK POPRAWKI ---
                // Po udanym utworzeniu użytkownika, uruchamiamy w tle wymuszoną
                // synchronizację listy użytkowników, aby nowy użytkownik był od razu dostępny.
                launch {
                    hdmLogger.log(context, "Uruchomiono synchronizację listy użytkowników po dodaniu nowego.")
                    LoginRepository.syncWithServer(context, loginApiService)
                }
                // --- KONIEC POPRAWKI ---

            }.onFailure { exception ->
                hdmLogger.log(
                    context,
                    logMessage = "Błąd tworzenia użytkownika '${currentState.login}': ${exception.message}",
                    level = LogLevel.ERROR // <-- POPRAWKA
                )
                _uiState.update {
                    it.copy(
                        creationState = CreateUserState.Error(
                            message = exception.message ?: "Nieznany błąd"
                        )
                    )
                }
            }
        }
    }

    fun resetCreationState() {
        _uiState.update { it.copy(creationState = CreateUserState.Idle) }
    }

    fun resetForm() {
        _uiState.value = CreateUserUiState()
    }
}