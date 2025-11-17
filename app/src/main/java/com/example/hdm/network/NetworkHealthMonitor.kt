// Ścieżka pliku: com/example/hdm/services/NetworkHealthMonitor.kt

package com.example.hdm.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.example.hdm.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HealthStatus(val message: String) {
    CHECKING("Sprawdzanie połączenia..."),
    OK("Połączono z siecią 'WMS'."),
    WRONG_WIFI("Błąd. Połącz się z siecią Wi-Fi o nazwie 'WMS'."),
    SERVER_ERROR("Serwer jest niedostępny. Skontaktuj się z administratorem."),
    NO_PERMISSION("Brak uprawnień do lokalizacji, aby sprawdzić sieć Wi-Fi."),
    LOCATION_DISABLED("Włącz usługi lokalizacji (GPS) w telefonie, aby zweryfikować sieć.")
}

object NetworkHealthMonitor {
    private val _status = MutableStateFlow(HealthStatus.CHECKING)
    val status = _status.asStateFlow()

    internal fun updateStatus(newStatus: HealthStatus) {
        _status.value = newStatus
    }

    fun checkWifiStatusNow(context: Context): HealthStatus {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        if (networkCapabilities == null || !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return HealthStatus.WRONG_WIFI
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val ssid = wifiManager.connectionInfo.ssid?.trim('"')

        return when {
            ssid.isNullOrBlank() -> HealthStatus.WRONG_WIFI
            ssid == "<unknown ssid>" -> HealthStatus.LOCATION_DISABLED
            ssid.equals("WMS", ignoreCase = true) -> HealthStatus.OK
            else -> HealthStatus.WRONG_WIFI
        }
    }

    suspend fun rerunChecks(context: Context, networkService: NetworkService, hdmLogger: HdmLogger) {
        Log.d("NetworkCheck", "Rozpoczynam sprawdzanie stanu sieci...")
        updateStatus(HealthStatus.CHECKING)

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        if (networkCapabilities == null || !networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.w("NetworkCheck", "Urządzenie nie jest połączone z siecią Wi-Fi. Ustawiam status: WRONG_WIFI")
            if (_status.value != HealthStatus.WRONG_WIFI) {
                hdmLogger.log(context, "Błąd sieci: Urządzenie nie jest połączone z siecią Wi-Fi.")
            }
            updateStatus(HealthStatus.WRONG_WIFI)
            return
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val ssid = wifiManager.connectionInfo.ssid

        Log.i("NetworkCheck", "Odczytano SSID: '$ssid'")
        val cleanedSsid = ssid?.trim('"')

        if (cleanedSsid.isNullOrBlank()) {
            Log.w("NetworkCheck", "SSID jest pusty lub null. Ustawiam status: WRONG_WIFI")
            if (_status.value != HealthStatus.WRONG_WIFI) {
                hdmLogger.log(context, "Błąd sieci: Nazwa połączonej sieci Wi-Fi (SSID) jest pusta.")
            }
            updateStatus(HealthStatus.WRONG_WIFI)
            return
        }

        if (cleanedSsid == "<unknown ssid>") {
            Log.w("NetworkCheck", "SSID to '<unknown ssid>'. Prawdopodobnie usługi lokalizacji są wyłączone. Ustawiam status: LOCATION_DISABLED")
            if (_status.value != HealthStatus.LOCATION_DISABLED) {
                hdmLogger.log(context, "Problem z siecią: Nie można odczytać nazwy Wi-Fi. Włącz usługi lokalizacji (GPS).")
            }
            updateStatus(HealthStatus.LOCATION_DISABLED)
            return
        }

        if (cleanedSsid.equals("WMS", ignoreCase = true)) {
            Log.d("NetworkCheck", "Nazwa sieci Wi-Fi jest poprawna. Ustawiam status: OK")
            if (_status.value != HealthStatus.OK) {
                hdmLogger.log(context, "Połączono z siecią 'WMS'. Status sieci: OK.")
            }
            updateStatus(HealthStatus.OK)
        } else {
            Log.w("NetworkCheck", "Nazwa sieci ('$cleanedSsid') jest inna niż 'WMS'. Ustawiam status: WRONG_WIFI")
            if (_status.value != HealthStatus.WRONG_WIFI) {
                hdmLogger.log(context, "Błąd sieci: Połączono z niewłaściwą siecią Wi-Fi ('$cleanedSsid').")
            }
            updateStatus(HealthStatus.WRONG_WIFI)
        }
    }
}