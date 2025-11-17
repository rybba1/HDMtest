package com.example.hdm.services

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.hdm.model.DnsLogRequest
import com.example.hdm.model.FileLogService
import com.example.hdm.model.LogEntry
import com.example.hdm.model.LogRepository
import com.example.hdm.model.UserManager
import com.example.hdm.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel {
    INFO, WARNING, ERROR, CHECK
}

@Singleton
class HdmLogger @Inject constructor(
    private val networkService: NetworkService,
    private val fileLogService: FileLogService
) {

    companion object {
        private const val TAG = "HdmLogger"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 5000L
    }

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    fun setSessionId(sessionId: String?) {
        _currentSessionId.value = sessionId
    }


    @SuppressLint("HardwareIds", "DefaultLocale")
    fun log(
        context: Context,
        logMessage: String,
        level: LogLevel = LogLevel.INFO,
        isBackgroundJob: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            val sessionId = _currentSessionId.value
            val finalLogMessage = if (sessionId != null) {
                "Session: $sessionId; $logMessage"
            } else {
                logMessage
            }

            val finalLevel = if (sessionId != null) {
                LogLevel.CHECK
            } else {
                level
            }

            launch {
                // --- POCZĄTEK ZBIERANIA DANYCH O URZĄDZENIU ---

                // 1. Marka i Model
                // Nie musimy nic robić, aby nazwa była z dużej litery,
                // producenci (Build.MANUFACTURER) zawsze podają ją z dużej.
                val marka = Build.MANUFACTURER
                val model = Build.MODEL
                val nazwaUrzadzenia = "$marka $model".replaceFirstChar { it.uppercase() }

                // 2. Android ID
                val androidId = Settings.Secure.getString(
                    context.applicationContext.contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: "ID_NIEDOSTEPNE"

                // 3. Wersja Androida
                val androidVersion = Build.VERSION.RELEASE

                // 4. Wersja Aplikacji (z poprawką na null)
                val appVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "VERSION_NULL"
                } catch (e: Exception) {
                    Log.w(TAG, "Nie udało się pobrać wersji aplikacji", e)
                    "VERSION_NIEDOSTEPNA"
                }

                // 5. Lokalne IP (Usunęliśmy RSSI)
                var localIp = "IP_NIEDOSTEPNY"
                @Suppress("DEPRECATION")
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val ipInt = wifiManager.connectionInfo.ipAddress
                    localIp = String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Nie udało się pobrać lokalnego IP", e)
                }

                // 6. Poziom Baterii (Usunęliśmy isCharging)
                var batteryLevel = -1
                try {
                    val batteryManager = context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                } catch (e: Exception) {
                    Log.w(TAG, "Nie udało się pobrać informacji o baterii", e)
                }

                // 7. Aktualny Użytkownik (Login)
                val userLoginForJson = if (isBackgroundJob) {
                    "SYSTEM"
                } else {
                    UserManager.getLoggedInUser()?.login ?: "BRAK_LOGOWANIA"
                }

                // --- KONIEC ZBIERANIA DANYCH O URZĄDZENIU ---

                val dnsLogRequest = DnsLogRequest(
                    deviceIp = localIp,
                    deviceName = nazwaUrzadzenia,
                    androidId = androidId,
                    androidVersion = androidVersion,
                    appVersion = appVersion,
                    batteryLevel = batteryLevel,
                    userLogin = userLoginForJson,
                    level = finalLevel.name,
                    message = finalLogMessage
                )

                // === LOGOWANIE DO LOGCAT ===
                Log.d("PAYLOAD_DEBUG", "Wysyłanie logu JSON: $dnsLogRequest")
                // ===========================

                val dnsResult = networkService.uploadDnsLog(dnsLogRequest)
                if (dnsResult.isSuccess) {
                    Log.i(TAG, "Alternatywny log (JSON) wysłany pomyślnie.")
                } else {
                    Log.w(TAG, "Błąd wysyłki alternatywnego logu (JSON): ${dnsResult.exceptionOrNull()?.message}")
                }
            }

            val logEntry = LogEntry(
                message = finalLogMessage,
                user = if (isBackgroundJob) "SYSTEM" else UserManager.currentUser.value
            )
            LogRepository.addLog(logEntry)
            fileLogService.appendLog(context, logEntry)

            var success = false
            for (attempt in 1..MAX_RETRIES) {
                try {
                    val xmlLog = LogXmlGenerator.generateLogXml(finalLogMessage, context, isBackgroundJob)

                    // === LOGOWANIE DO LOGCAT ===
                    Log.d("PAYLOAD_DEBUG", "Wysyłanie logu XML (próba $attempt): $xmlLog")
                    // ===========================

                    val result = networkService.uploadXmlReport(xmlLog)

                    if (result.isSuccess) {
                        logEntry.isSent.value = true
                        Log.i(TAG, "Log '$finalLogMessage' wysłany pomyślnie (próba $attempt/$MAX_RETRIES).")

                        val allLogs = fileLogService.readLogs(context).toMutableList()
                        val index = allLogs.indexOfFirst { it.id == logEntry.id }
                        if (index != -1) {
                            val updatedLog = allLogs[index].copy().apply { isSent.value = true }
                            allLogs[index] = updatedLog
                            fileLogService.updateLogs(context, allLogs)
                            LogRepository.updateLogEntry(updatedLog)
                        }
                        success = true
                        break
                    } else {
                        Log.w(TAG, "Nie udało się wysłać logu '$finalLogMessage' (próba $attempt/$MAX_RETRIES). Błąd: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Krytyczny błąd podczas próby wysłania logu (próba $attempt/$MAX_RETRIES).", e)
                }
                if (attempt < MAX_RETRIES) {
                    delay(RETRY_DELAY_MS)
                }
            }

            if (!success) {
                Log.e(TAG, "Nie udało się wysłać logu '$finalLogMessage' po $MAX_RETRIES próbach. Log zostaje w kolejce dla automatu.")
            }
        }
    }
}