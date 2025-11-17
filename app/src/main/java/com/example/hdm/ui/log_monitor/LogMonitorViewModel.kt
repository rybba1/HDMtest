package com.example.hdm.ui.log_monitor

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.hdm.model.ArchiveRepository
import com.example.hdm.model.ArchivedSession
import com.example.hdm.model.LogEntry
import com.example.hdm.model.LogRepository
import com.example.hdm.network.NetworkService
import com.example.hdm.services.ConnectivityMonitor
import com.example.hdm.services.FileService
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.LogSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- DATA CLASS DO PARSOWANIA ODPOWIEDZI PING ---
@Serializable
private data class MinimalPingResponse(
    @SerialName("processing_time_ms")
    val processingTimeMs: Double? = null,
    @SerialName("network_info")
    val networkInfo: NetworkInfoResponse? = null
)

@Serializable
private data class NetworkInfoResponse(
    @SerialName("client_ip")
    val clientIp: String? = null,
    @SerialName("client_port")
    val clientPort: String? = null
)
// ---------------------------------------------


@HiltViewModel
class LogMonitorViewModel @Inject constructor(
    private val application: Application,
    private val fileService: FileService,
    private val networkService: NetworkService,
    private val connectivityMonitor: ConnectivityMonitor,
    private val hdmLogger: HdmLogger
) : AndroidViewModel(application) {

    // --- Logi aplikacyjne / archiwum raportów ---
    val logs: StateFlow<List<LogEntry>> = LogRepository.logs
    private val _archivedSessions = MutableStateFlow<List<ArchivedSession>>(emptyList())
    val archivedSessions: StateFlow<List<ArchivedSession>> = _archivedSessions.asStateFlow()

    // --- Stan diagnostyki sieci ---
    private val _isPingingActive = MutableStateFlow(false)
    val isPingingActive: StateFlow<Boolean> = _isPingingActive.asStateFlow()

    private val _diagnosticEvents = MutableStateFlow<List<DiagnosticEvent>>(emptyList())

    private val _diagnosticConfig = MutableStateFlow(DiagnosticConfig(retryStrategy = RetryStrategy.EXPONENTIAL, throttleDelayMs = 2000))
    val diagnosticConfig: StateFlow<DiagnosticConfig> = _diagnosticConfig.asStateFlow()

    private val _statistics = MutableStateFlow(PingStatistics())
    val statistics: StateFlow<PingStatistics> = _statistics.asStateFlow()

    private var pingJob: Job? = null
    private var nextPingId = 1
    private var retryHelper = RetryHelper(_diagnosticConfig.value)
    // ---------------------------------

    private val json = Json { ignoreUnknownKeys = true }

    // Połączone logi Pingu i Zdarzeń Sieciowych
    val diagnosticEvents: StateFlow<List<DiagnosticEvent>> =
        _diagnosticEvents.combine(connectivityMonitor.events) { pings, networkEvents ->
            // === POCZĄTEK POPRAWKI ===
            // 'pings' to już 'List<DiagnosticEvent>' (zawierająca PingEvent)
            // Nie mapujemy jej ponownie, tylko łączymy z nowymi zdarzeniami sieciowymi.
            val combined = pings + networkEvents.map { NetworkStateEvent(it) }
            // === KONIEC POPRAWKI ===
            combined.sortedByDescending { it.timestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val fileDateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val logDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    init {
        loadArchivedSessions()
    }

    override fun onCleared() {
        connectivityMonitor.stopMonitoring()
        super.onCleared()
    }

    // --- Funkcje Archiwum i Logów (bez zmian) ---
    fun loadArchivedSessions() {
        _archivedSessions.value = ArchiveRepository.getArchivedSessions(getApplication())
    }

    fun deleteArchivedSession(archiveId: String) {
        viewModelScope.launch {
            ArchiveRepository.deleteArchive(getApplication(), archiveId)
            hdmLogger.log(getApplication(), "Usunięto zarchiwizowany raport: $archiveId", isBackgroundJob = true)
            loadArchivedSessions()
        }
    }

    fun triggerLogSync() {
        val logSyncRequest = OneTimeWorkRequestBuilder<LogSyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "ManualLogSync",
            ExistingWorkPolicy.KEEP,
            logSyncRequest
        )
    }

    // --- Funkcje Diagnostyki (zintegrowane) ---

    fun updateDiagnosticConfig(newConfig: DiagnosticConfig) {
        _diagnosticConfig.value = newConfig
        retryHelper = RetryHelper(newConfig).also { it.resetCircuitBreaker() }
        _statistics.value = PingStatistics()
        _diagnosticEvents.value = emptyList()
        nextPingId = 1
    }

    @Suppress("DEPRECATION")
    fun startContinuousPing() {
        if (_isPingingActive.value) return
        connectivityMonitor.startMonitoring()
        _isPingingActive.value = true
        pingJob = viewModelScope.launch {
            while (isActive) {
                // === POCZĄTEK POPRAWKI: Dane sieciowe muszą być pobierane w pętli ===
                val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val connectionInfo: WifiInfo? = wifiManager.connectionInfo
                val network = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(network)

                val ssid = connectionInfo?.ssid?.trim('"')
                val rssi = connectionInfo?.rssi
                val bssid = connectionInfo?.bssid
                val linkSpeed = connectionInfo?.linkSpeed
                val frequency = connectionInfo?.frequency
                val downstream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) caps?.linkDownstreamBandwidthKbps else null
                val upstream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) caps?.linkUpstreamBandwidthKbps else null
                // === KONIEC POPRAWKI ===

                performSinglePing(ssid, rssi, bssid, linkSpeed, frequency, downstream, upstream)

                val throttle = _diagnosticConfig.value.throttleDelayMs
                if (throttle > 0) {
                    delay(throttle)
                }
            }
        }
    }

    fun stopContinuousPing() {
        _isPingingActive.value = false
        pingJob?.cancel()
        pingJob = null
        connectivityMonitor.stopMonitoring()
    }

    fun clearDiagnosticLogs() {
        _diagnosticEvents.value = emptyList()
        _statistics.value = PingStatistics()
        nextPingId = 1
        retryHelper.resetCircuitBreaker()
        connectivityMonitor.stopMonitoring()
        connectivityMonitor.startMonitoring()
    }

    private suspend fun performSinglePing(
        ssid: String?, rssi: Int?, bssid: String?, linkSpeed: Int?, frequency: Int?, downstream: Int?, upstream: Int?
    ) {
        val id = nextPingId++
        val startTime = System.currentTimeMillis()

        // Używamy RetryHelper do opakowania *prawdziwego* wywołania sieciowego
        val (result, retryAttempts) = retryHelper.executeWithRetry {
            // .getOrThrow() jest kluczowe - RetryHelper sam łapie wyjątki
            networkService.pingServer(getApplication()).getOrThrow()
        }

        val endTime = System.currentTimeMillis()
        val totalRtt = endTime - startTime

        val pingLog: PingLog = if (result.isSuccess) {
            val pingData = result.getOrThrow() // 'pingData' to 'PingResult'
            var serverTime: Double? = null
            var clientIp: String? = null
            var clientPort: String? = null

            try {
                // Parsujemy odpowiedź, którą teraz poprawnie otrzymujemy
                val serverResponse = json.decodeFromString<MinimalPingResponse>(pingData.rawResponse)
                serverTime = serverResponse.processingTimeMs
                clientIp = serverResponse.networkInfo?.clientIp
                clientPort = serverResponse.networkInfo?.clientPort
            } catch (e: Exception) {
                Log.w("LogMonitorVM", "Nie można sparsować odpowiedzi JSON pingu: ${e.message}")
            }

            PingLog(
                id = id,
                timestamp = endTime,
                roundTripTimeMs = pingData.roundTripTimeMs, // Czas ostatniego żądania
                status = PingStatus.SUCCESS,
                message = "OK (${pingData.httpCode})",
                serverResponse = pingData.rawResponse,
                serverProcessingTimeMs = serverTime,
                clientIp = clientIp,
                clientPort = clientPort,
                ssid = ssid, rssi = rssi, bssid = bssid, linkSpeed = linkSpeed, frequency = frequency, downstreamBw = downstream, upstreamBw = upstream,
                retryAttempts = retryAttempts,
                totalTime = totalRtt
            )
        } else {
            val ex = result.exceptionOrNull()
            val status = if (ex is SocketTimeoutException) PingStatus.TIMEOUT else PingStatus.FAILED
            PingLog(
                id = id,
                timestamp = endTime,
                roundTripTimeMs = totalRtt,
                status = status,
                message = ex?.message ?: status.name,
                serverResponse = ex?.stackTraceToString(),
                serverProcessingTimeMs = null,
                clientIp = null,
                clientPort = null,
                ssid = ssid, rssi = rssi, bssid = bssid, linkSpeed = linkSpeed, frequency = frequency, downstreamBw = downstream, upstreamBw = upstream,
                retryAttempts = retryAttempts,
                totalTime = totalRtt
            )
        }

        appendDiagnosticEvent(PingEvent(pingLog))
        updateStatistics(pingLog)
    }

    private fun appendDiagnosticEvent(event: DiagnosticEvent) {
        _diagnosticEvents.value = (listOf(event) + _diagnosticEvents.value).take(10000)
    }

    private fun updateStatistics(log: PingLog) {
        val current = _statistics.value
        val newTotal = current.totalRequests + 1
        val newSuccess = current.successfulRequests + if (log.status == PingStatus.SUCCESS) 1 else 0
        val newFailed = current.failedRequests + if (log.status != PingStatus.SUCCESS) 1 else 0
        val newRetries = current.totalRetries + log.retryAttempts

        val newAvgTime = if (newSuccess == 0) 0
        else if (log.status == PingStatus.SUCCESS) {
            ((current.averageResponseTime * current.successfulRequests) + log.roundTripTimeMs) / newSuccess
        } else {
            current.averageResponseTime
        }

        _statistics.value = current.copy(
            totalRequests = newTotal,
            successfulRequests = newSuccess,
            failedRequests = newFailed,
            totalRetries = newRetries,
            averageResponseTime = newAvgTime
        )
    }

    // === POPRAWIONA FUNKCJA savePingLogsToFile ===
    fun savePingLogsToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            val eventsToSave = diagnosticEvents.value // Pobierz aktualną listę DiagnosticEvent
            if (eventsToSave.isEmpty()) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Brak logów do zapisania.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val fileContent = StringBuilder("Dziennik Diagnostyczny - ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            fileContent.append("Konfiguracja: Strategia=${_diagnosticConfig.value.retryStrategy}, MaxPrób=${_diagnosticConfig.value.maxRetries}, Opóźnienie=${_diagnosticConfig.value.baseDelayMs}ms, Throttling=${_diagnosticConfig.value.throttleDelayMs}ms\n\n")

            // Iteruj po pełnej liście i filtruj typy
            eventsToSave.reversed().forEach { event ->
                val date = logDateFormatter.format(Date(event.timestamp))

                when(event) {
                    is PingEvent -> {
                        val log = event.log // Teraz 'log' to PingLog
                        val latencyText = when(log.status) {
                            PingStatus.SUCCESS -> "${log.roundTripTimeMs} ms (Serwer: ${log.serverProcessingTimeMs?.let { "%.1f ms".format(it) } ?: "N/A"})"
                            PingStatus.TIMEOUT -> log.message
                            PingStatus.FAILED -> "Błąd"
                        }
                        val rssiText = log.rssi?.let { "$it dBm" } ?: "N/A"
                        val speed = log.linkSpeed?.let { "$it Mbps" } ?: "N/A"
                        val freq = log.frequency?.let { if(it > 4900) "5 GHz" else "2.4 GHz" } ?: "N/A"
                        val downBw = log.downstreamBw?.let { "${it/1000} Mbps"} ?: "N/A"
                        val upBw = log.upstreamBw?.let { "${it/1000} Mbps"} ?: "N/A"

                        fileContent.append("[$date] PING: Status: ${log.status}, Czas: $latencyText\n")
                        fileContent.append("     -> Info: SSID: ${log.ssid ?: "N/A"}, BSSID: ${log.bssid ?: "N/A"}\n")
                        fileContent.append("     -> Klient: IP: ${log.clientIp ?: "N/A"}, Port: ${log.clientPort ?: "N/A"}\n")
                        fileContent.append("     -> Sieć: Siła: $rssiText, Prędkość: $speed, Pasmo: $freq, Pob: $downBw, Wys: $upBw\n")
                        fileContent.append("     -> Komunikat: ${log.message}\n")

                        if (log.retryAttempts > 0) {
                            fileContent.append("     -> Ponowienia: ${log.retryAttempts} (Całkowity czas: ${log.totalTime}ms)\n")
                        }

                        if (log.status != PingStatus.SUCCESS || log.serverProcessingTimeMs == null) {
                            val responseToShow = if (log.status == PingStatus.FAILED) log.message else log.serverResponse
                            fileContent.append("     -> Odpowiedź: ${responseToShow?.replace("\n", " ")?.take(200)}\n")
                        }
                        fileContent.append("\n") // Dodatkowy odstęp
                    }
                    is NetworkStateEvent -> {
                        fileContent.append("[$date] ZDARZENIE SIECIOWE: ${event.event.message}\n\n")
                    }
                }
            }
            // === KONIEC POPRAWKI ===

            val timestamp = fileDateFormatter.format(Date())
            val fileName = "HDM_Diagnostics_Log_$timestamp.txt"
            val result = fileService.saveTextToDownloads(getApplication(), fileContent.toString(), fileName)

            launch(Dispatchers.Main) {
                result.onSuccess {
                    Toast.makeText(getApplication(), "Zapisano logi: $it", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(getApplication(), "Błąd zapisu: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}