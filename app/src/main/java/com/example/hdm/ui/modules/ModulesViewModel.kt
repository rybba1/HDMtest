// ZASTĄP CAŁY PLIK: com/example/hdm/ui/modules/ModulesViewModel.kt
package com.example.hdm.ui.modules

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.network.NetworkService
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.NetworkHealthMonitor
import com.example.hdm.services.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModulesUiState(
    val showPasswordDialog: Boolean = false,
    val passwordError: Boolean = false
)

@HiltViewModel
class ModulesViewModel @Inject constructor(
    private val application: Application,
    private val networkService: NetworkService,
    private val hdmLogger: HdmLogger,
    private val updateManager: UpdateManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ModulesUiState())
    val uiState = _uiState.asStateFlow()

    val updateState = updateManager.updateState

    private val _navigationEvent = Channel<String>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private var destinationRoute: String? = null

    private companion object {
        const val CORRECT_PASSWORD_1 = "Logistyka2025!"
        const val CORRECT_PASSWORD_2 = "adi123"
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }

    // === POPRAWKA ===
    // Funkcja przyjmuje `filename` jako String i przekazuje go do menedżera.
    // Usunięto błędny argument `application`.
    fun startUpdateDownload(filename: String) {
        updateManager.startDownload(filename)
    }
    // ================

    fun resetUpdateState() {
        updateManager.resetState()
    }

    fun onModuleClicked(module: Module, context: Context) {
        val protectedModuleIds = setOf("label_printing", "nagoya_report", "pallet_lookup")

        if (module.id in protectedModuleIds) {
            destinationRoute = module.route
            _uiState.update { it.copy(showPasswordDialog = true) }
        } else {
            module.route?.let { route ->
                hdmLogger.log(context, "Otwarto moduł: ${module.title}")
                viewModelScope.launch {
                    _navigationEvent.send(route)
                }
            }
        }
    }

    fun onPasswordConfirm(password: String, context: Context) {
        if (password == CORRECT_PASSWORD_1 || password == CORRECT_PASSWORD_2) {
            destinationRoute?.let { route ->
                hdmLogger.log(context, "Autoryzowano dostęp do modułu: $route")
                viewModelScope.launch {
                    _navigationEvent.send(route)
                }
            }
            dismissPasswordDialog()
        } else {
            _uiState.update { it.copy(passwordError = true) }
        }
    }

    fun dismissPasswordDialog() {
        destinationRoute = null
        _uiState.update { it.copy(showPasswordDialog = false, passwordError = false) }
    }

    fun rerunNetworkChecks(context: Context) {
        viewModelScope.launch {
            NetworkHealthMonitor.rerunChecks(context, networkService, hdmLogger)
        }
    }
}