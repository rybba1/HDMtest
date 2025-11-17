package com.example.hdm.ui.splash

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.model.LogRepository
import com.example.hdm.model.UserManager
import com.example.hdm.network.NetworkService
import com.example.hdm.services.DownloadState
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.NetworkHealthMonitor
import com.example.hdm.services.TranslationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.hdm.services.UpdateManager
enum class SplashUiState {
    INITIAL_LOADING,
    REQUIRES_GUEST_WIFI,
    DOWNLOADING_MODELS,
    REQUIRES_WMS_WIFI,
    ALL_DONE
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val application: Application,
    private val networkService: NetworkService,
    private val hdmLogger: HdmLogger,
    val translationService: TranslationService,
    private val updateManager: UpdateManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SplashUiState.INITIAL_LOADING)
    val uiState = _uiState.asStateFlow()

    private val _openWifiSettingsEvent = Channel<Unit>(Channel.CONFLATED)
    val openWifiSettingsEvent = _openWifiSettingsEvent.receiveAsFlow()

    fun triggerStartupTasks() {
        viewModelScope.launch {
            UserManager.initialize(application)
            LogRepository.initialize(application)
            NetworkHealthMonitor.rerunChecks(application, networkService, hdmLogger)
            updateManager.checkForUpdates()
            delay(1500)

            // Najpierw sprawdź, czy modele już są
            if (translationService.downloadState.value == DownloadState.COMPLETED) {
                _uiState.value = SplashUiState.ALL_DONE
                return@launch
            }

            // Teraz sprawdź sieć
            val currentSsid = getCurrentSsid(application)
            if (currentSsid != null && currentSsid.equals("WMS", ignoreCase = true)) {
                // Jesteśmy w WMS bez modeli - prosimy o zmianę
                _uiState.value = SplashUiState.REQUIRES_GUEST_WIFI
            } else {
                // Jesteśmy w innej sieci (z internetem) lub nie jesteśmy w Wi-Fi - próbujemy pobrać
                startModelDownload()
            }
        }
    }

    private fun getCurrentSsid(context: Context): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        return if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo.ssid?.trim('"')
        } else {
            null // Nie jesteśmy połączeni z Wi-Fi
        }
    }

    fun requestOpenWifiSettings() {
        viewModelScope.launch {
            _openWifiSettingsEvent.send(Unit)
        }
    }

    fun startModelDownload() {
        _uiState.value = SplashUiState.DOWNLOADING_MODELS
        viewModelScope.launch {
            translationService.ensureModelsDownloaded()
            if (translationService.downloadState.value == DownloadState.COMPLETED) {
                // Po pobraniu od razu prosimy o powrót do WMS
                _uiState.value = SplashUiState.REQUIRES_WMS_WIFI
            } else {
                // Jeśli pobieranie się nie udało, wracamy do prośby o połączenie
                _uiState.value = SplashUiState.REQUIRES_GUEST_WIFI
            }
        }
    }

    fun checkIfOnWmsAndProceed() {
        val currentSsid = getCurrentSsid(application)
        if (currentSsid != null && currentSsid.equals("WMS", ignoreCase = true)) {
            _uiState.value = SplashUiState.ALL_DONE
        }
    }
}