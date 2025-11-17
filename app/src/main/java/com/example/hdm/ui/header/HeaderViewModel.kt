package com.example.hdm.ui.header

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.hdm.Screen
import com.example.hdm.model.ArchiveRepository
import com.example.hdm.model.ArchivedSession
import com.example.hdm.model.Base64Attachment
import com.example.hdm.model.DamageDetail
import com.example.hdm.model.DamageInfo
import com.example.hdm.model.DamageInstance
import com.example.hdm.model.DamagedPallet
import com.example.hdm.model.PalletPosition
import com.example.hdm.model.SessionDetailsResponse
import com.example.hdm.model.RawMaterialRepository
import com.example.hdm.model.ReportHeader
import com.example.hdm.model.UploadStatus
import com.example.hdm.model.UserManager
import com.example.hdm.model.strategies.ReportStrategyFactory
import com.example.hdm.model.strategies.ReportWorkflowStrategy
import com.example.hdm.services.CodeValidator
import com.example.hdm.services.DescriptionGenerator
import com.example.hdm.services.DirectSessionApiService
import com.example.hdm.services.ExternalBarcodeApiService
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.ImageUtils
import com.example.hdm.services.LogLevel
import com.example.hdm.services.TranslationService
import com.example.hdm.ui.placement.DamageMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import com.example.hdm.model.ImageUploadResponse
import com.example.hdm.model.SessionPalletData
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.FileOutputStream
import com.example.hdm.R

// --- Modele Barcode (bez zmian) ---
data class BarcodeData(val rodzajTowaru: String, val numerLotu: String)
sealed class BarcodeFetchState {
    object Idle : BarcodeFetchState()
    object Loading : BarcodeFetchState()
    data class Success(val data: BarcodeData) : BarcodeFetchState()
    data class RequiresSelection(val options: List<BarcodeData>) : BarcodeFetchState()
    data class Error(val message: String) : BarcodeFetchState()
}
// --- Koniec Modeli Barcode ---

// === ZAKTUALIZOWANE STANY WYSYŁANIA ===
sealed class SubmissionState {
    object Idle : SubmissionState()
    data class InProgress(val message: String) : SubmissionState()
    data class Generating(val step: ProcessingStep, val progress: Float) : SubmissionState()
    object FinalizeSuccess : SubmissionState()
    data class Success(val message: String) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

enum class ProcessingStep {
    PREPARING_DATA,
    SENDING_COMMENTS,
    GENERATING_PDF,
    UPLOADING_PDF,
    VERIFYING_DATA,
    FINALIZING
}
// ========================================

// ===== NOWE MODELE DLA ULEPSZEŃ =====
sealed class BackgroundPdfState {
    object Idle : BackgroundPdfState()
    object Generating : BackgroundPdfState()
    data class Ready(val pdfPlPath: String, val pdfEnPath: String) : BackgroundPdfState()
    data class Error(val message: String) : BackgroundPdfState()
}

data class SyncReport(
    var headerSynced: Boolean = false,
    var palletsSynced: Int = 0,
    var palletsSkipped: Int = 0,
    var layoutSynced: Boolean = false,
    val warnings: MutableList<String> = mutableListOf(),
    val errors: MutableList<String> = mutableListOf()
)
// ====================================

@HiltViewModel
class HeaderViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val pdfGenerator: PdfReportGenerator,
    private val translationService: TranslationService,
    private val hdmLogger: HdmLogger,
    private val externalBarcodeApiService: ExternalBarcodeApiService,
    private val directSessionApiService: DirectSessionApiService,
    private val json: Json
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HeaderViewModel"
    }

    // ===== NOWY MUTEX =====
    // Mutex do ochrony przed jednoczesnym pobieraniem indeksu palety
    private val indexFetchMutex = Mutex()
    // ======================

    private val _pendingSessions = MutableStateFlow<List<SessionDetailsResponse>>(emptyList())
    val pendingSessions: StateFlow<List<SessionDetailsResponse>> = _pendingSessions.asStateFlow()

    private val _isSessionLoading = MutableStateFlow(false)
    val isSessionLoading: StateFlow<Boolean> = _isSessionLoading.asStateFlow()

    // ===== POCZĄTEK NOWEGO KODU (DO PODGLĄDU) =====

    private val _detailedSession = MutableStateFlow<SessionDetailsResponse?>(null)
    /** Przechowuje pełne dane sesji (wraz z załącznikami) pobrane na żądanie. */
    val detailedSession: StateFlow<SessionDetailsResponse?> = _detailedSession.asStateFlow()

    private val _isDetailedSessionLoading = MutableStateFlow(false)
    /** Wskazuje, czy trwa ładowanie pełnych szczegółów sesji. */
    val isDetailedSessionLoading: StateFlow<Boolean> = _isDetailedSessionLoading.asStateFlow()

    /**
     * Pobiera pełne szczegóły sesji (w tym załączniki Base64) na żądanie.
     * Używane do podglądu w ReportTypeScreen.
     */
    fun loadDetailedSession(sessionId: String) {
        viewModelScope.launch {
            _isDetailedSessionLoading.value = true
            _detailedSession.value = null // Wyczyść poprzednie dane
            val result = directSessionApiService.getSessionDetails(sessionId)

            result.onSuccess { details ->
                _detailedSession.value = details
            }.onFailure {
                Toast.makeText(application, "Błąd wczytania szczegółów: ${it.message}", Toast.LENGTH_SHORT).show()
            }
            _isDetailedSessionLoading.value = false
        }
    }

    /**
     * Czyści pobrane szczegóły sesji z pamięci, gdy okno dialogowe jest zamykane.
     */
    fun clearDetailedSession() {
        _detailedSession.value = null
    }

    // ===== KONIEC NOWEGO KODU (DO PODGLĄDU) =====

    val currentSessionId: StateFlow<String?> = savedStateHandle.getStateFlow("sessionId", null)

    private val _barcodeFetchState = MutableStateFlow<BarcodeFetchState>(BarcodeFetchState.Idle)
    val barcodeFetchState: StateFlow<BarcodeFetchState> = _barcodeFetchState.asStateFlow()
    private var barcodeFetchJob: Job? = null

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    private val _strategy = MutableStateFlow<ReportWorkflowStrategy?>(null)
    val strategy: StateFlow<ReportWorkflowStrategy?> = _strategy.asStateFlow()

    val reportHeaderState: StateFlow<ReportHeader> =
        savedStateHandle.getStateFlow("reportHeader", ReportHeader())
    val currentPalletState: StateFlow<DamagedPallet> =
        savedStateHandle.getStateFlow("currentPallet", DamagedPallet())
    val savedPallets: StateFlow<List<DamagedPallet>> =
        savedStateHandle.getStateFlow("savedPallets", emptyList())
    val palletPositions: StateFlow<List<PalletPosition>> =
        savedStateHandle.getStateFlow("palletPositions", emptyList())
    val selectedVehicleLayoutState: StateFlow<String?> =
        savedStateHandle.getStateFlow("selectedVehicleLayout", null)
    val damageMarkersState: StateFlow<Map<String, List<DamageMarker>>> =
        savedStateHandle.getStateFlow("damageMarkers", emptyMap())
    val damageHeightSelections: StateFlow<Map<String, Map<String, Set<String>>>> =
        savedStateHandle.getStateFlow("damageHeightSelections", emptyMap())

    private val _placementValidationActive = MutableStateFlow(false)
    val placementValidationActive: StateFlow<Boolean> = _placementValidationActive.asStateFlow()

    private val _isNagoyaFlow = MutableStateFlow(false)
    val isNagoyaFlow: StateFlow<Boolean> = _isNagoyaFlow.asStateFlow()

    val currentScreenRoute: StateFlow<String?> =
        savedStateHandle.getStateFlow("currentScreenRoute", null)

    private val _logoutAndNavigateToLogin = Channel<Unit>(Channel.CONFLATED)
    val logoutAndNavigateToLogin = _logoutAndNavigateToLogin.receiveAsFlow()

    private val _navigateToSummary = Channel<Unit>(Channel.CONFLATED)
    val navigateToSummary = _navigateToSummary.receiveAsFlow()

    // ===== NOWE STANY DLA PDF W TLE =====
    private val _backgroundPdfState = MutableStateFlow<BackgroundPdfState>(BackgroundPdfState.Idle)
    val backgroundPdfState: StateFlow<BackgroundPdfState> = _backgroundPdfState.asStateFlow()
    private var backgroundPdfJob: Job? = null
    // ====================================

    init {
        val restoredReportType = reportHeaderState.value.reportType
        if (restoredReportType.isNotBlank() && _strategy.value == null) {
            _strategy.value = ReportStrategyFactory.create(restoredReportType)
        }
        currentSessionId.value?.let { hdmLogger.setSessionId(it) }
    }

    // ===== LOGIKA SESJI (bez zmian) =====

    fun updateReportType(type: String) {
        fullReset()
        _strategy.value = ReportStrategyFactory.create(type)

        val newSessionId = DirectSessionApiService.generateSessionId()

        savedStateHandle["sessionId"] = newSessionId
        hdmLogger.setSessionId(newSessionId)

        val loggedInUser = UserManager.getLoggedInUser()
        val workerName = loggedInUser?.fullName ?: "Brak danych"
        val workerId = loggedInUser?.workerId
        val timestamp = System.currentTimeMillis()

        savedStateHandle["reportHeader"] = ReportHeader(
            reportType = type,
            magazynier = workerName,
            workerId = workerId,
            dataGodzina = timestamp
        )

        setCurrentScreen(Screen.Header)
        hdmLogger.log(application, "NOWA SESJA ($newSessionId): Typ: '$type', User: '$workerName'")

        viewModelScope.launch(Dispatchers.IO) {
            val data = mapOf(
                "header_data.report_type" to type,
                "header_data.pic_wh" to workerName,
                "header_data.report_datetime" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            )
            val result = directSessionApiService.updateSession(newSessionId, data)
            if (result.isFailure) {
                Log.w(TAG, "Pierwsza aktualizacja sesji ($newSessionId) nie powiodła się: ${result.exceptionOrNull()?.message}")
            } else {
                Log.i(TAG, "Sesja ($newSessionId) pomyślnie zainicjowana na serwerze.")
            }
        }
    }

    private fun buildHeaderDataMap(): Map<String, String> {
        val header = reportHeaderState.value
        val data = mutableMapOf<String, String>()

        if (header.rodzajSamochodu in listOf("OKTRANS", "ROTONDO", "DSV")) {
            data["header_data.truck_number"] = header.rodzajSamochodu
        } else {
            val customTruckNumber = when (header.rodzajSamochodu) {
                "Bus", "Solo" -> header.numerAuta
                "Naczepa", "Tandem" ->
                    if (header.numerNaczepyKontenera.isNotBlank()) "${header.numerAuta}/${header.numerNaczepyKontenera}"
                    else header.numerAuta
                else -> ""
            }
            if (customTruckNumber.isNotBlank()) {
                data["header_data.truck_number_custom"] = customTruckNumber
            }
        }

        val placeApiValue = when (header.miejsce) {
            "Leon" -> "WH1 Wyczółkowskiego 121"
            "Gaudi" -> "WH2 Gaudiego 6D"
            else -> header.miejsce
        }
        data["header_data.place"] = placeApiValue

        // ===== POPRAWKA (Z POPRZEDNIEJ ODPOWIEDZI) =====
        // Zapisujemy RZECZYWISTY rodzaj samochodu, który był tracony
        if (header.rodzajSamochodu.isNotBlank()) {
            data["header_data.vehicle_type"] = header.rodzajSamochodu
        }
        // ===== KONIEC POPRAWKI =====

        data["header_data.warehouse_location"] = header.lokalizacja
        data["header_data.feniks_ref_no"] = header.numerDokumentuFeniks
        data["header_data.container_number"] = if (header.rodzajSamochodu == "Kontener") header.numerKontenera else ""

        val reportMode = when (header.rodzajPalet) {
            "Surowiec" -> "RM"
            "Wyrób gotowy" -> "FP"
            "Inne" -> "INNE"
            else -> ""
        }
        data["header_data.report_mode_at_save"] = reportMode
        data["header_data.forklift_type"] = header.rodzajWozka
        data["header_data.report_timestamp_long"] = header.dataGodzina.toString()
        return data
    }

    fun sendHeaderData() {
        val sessionId = currentSessionId.value
        if (sessionId == null) {
            Log.e(TAG, "sendHeaderData: Brak ID sesji!")
            return
        }

        hdmLogger.log(application, "Wysyłanie danych nagłówka dla sesji $sessionId")

        viewModelScope.launch(Dispatchers.IO) {
            val data = buildHeaderDataMap()

            if (data.isNotEmpty()) {
                val result = directSessionApiService.updateSession(sessionId, data)
                if (result.isFailure) {
                    Log.w(TAG, "Aktualizacja nagłówka ($sessionId) nie powiodła się: ${result.exceptionOrNull()?.message}")
                } else {
                    Log.i(TAG, "Aktualizacja nagłówka ($sessionId) pomyślna.")
                }
            }
        }
    }

    // ===== POCZĄTEK NOWEJ LOGIKI WYSYŁANIA (ASYNC) =====

    /**
     * Nowa "bramka" do pobierania indeksu palety.
     * Zapewnia, że indeks zostanie pobrany tylko raz dla nowej palety.
     * Zwraca `null`, jeśli pobieranie się nie powiedzie.
     */
    private suspend fun getOrFetchPalletIndex(): Int? {
        val sessionId = currentSessionId.value
        if (sessionId == null) {
            Log.e(TAG, "getOrFetchPalletIndex: Brak ID sesji!")
            return null
        }

        val existingIndex = currentPalletState.value.serverIndex
        if (existingIndex != null) {
            return existingIndex
        }

        indexFetchMutex.withLock {
            val currentIndexAfterLock = currentPalletState.value.serverIndex
            if (currentIndexAfterLock != null) {
                return currentIndexAfterLock
            }

            Log.i(TAG, "Pobieranie nowego indeksu palety z serwera...")
            val summaryResult = directSessionApiService.getSessionSummary(sessionId)

            if (summaryResult.isSuccess) {
                val palletIndex = summaryResult.getOrThrow().palletCount
                Log.i(TAG, "Pobrano nowy indeks: $palletIndex. Zapisuję do currentPallet.")

                savedStateHandle["currentPallet"] = currentPalletState.value.copy(serverIndex = palletIndex)
                return palletIndex
            } else {
                Log.e(TAG, "Nie można pobrać /summary: ${summaryResult.exceptionOrNull()?.message}")
                updateAllPhotoStatuses(UploadStatus.FAILED)
                return null
            }
        }
    }

    /**
     * Nowa główna funkcja do wysyłania zdjęć w tle.
     * Wywoływana z UI (launcher aparatu, przycisk ponawiania).
     */
    fun uploadPhotoInBackground(
        imageType: String,
        pathString: String,
        damageInstanceId: String? // null dla nowych zdjęć uszkodzeń, ID dla ponawiania
    ) {
        val sessionId = currentSessionId.value ?: return

        viewModelScope.launch {
            val palletIndex = getOrFetchPalletIndex()

            if (palletIndex == null) {
                Log.e(TAG, "uploadPhotoInBackground: Nie udało się pobrać indeksu. Przerywam wysyłanie zdjęcia.")
                return@launch
            }

            launch(Dispatchers.IO) {

                val (updatedPallet, instanceIdToUpdate) = updatePalletStateForUploadStart(
                    imageType = imageType,
                    pathString = pathString,
                    damageInstanceId = damageInstanceId
                )

                val result = uploadImageHelper(
                    sessionId = sessionId,
                    palletIndex = palletIndex,
                    imageType = imageType,
                    pathString = pathString,
                    existingFileId = getExistingFileId(updatedPallet, imageType, instanceIdToUpdate)
                )

                updatePalletStateForUploadResult(
                    result = result,
                    imageType = imageType,
                    damageInstanceId = instanceIdToUpdate
                )
            }
        }
    }

    /** Helper do `uploadPhotoInBackground`: Zwraca FileId, jeśli już istnieje dla danego zdjęcia. */
    private fun getExistingFileId(pallet: DamagedPallet, imageType: String, damageInstanceId: String?): String? {
        return when (imageType) {
            "PhotoLabel" -> pallet.zdjecieDuzejEtykietyFileId
            "PhotosOverview" -> pallet.zdjecieCalejPaletyFileId
            "PhotosDamage" -> pallet.damageInstances.find { it.id == damageInstanceId }?.serverFileId
            else -> null
        }
    }

    /** Helper do `uploadPhotoInBackground`: Aktualizuje stan na UPLOADING i zwraca zaktualizowaną paletę. */
    private fun updatePalletStateForUploadStart(
        imageType: String,
        pathString: String,
        damageInstanceId: String?
    ): Pair<DamagedPallet, String?> {
        var newInstanceId: String? = null
        val updatedPallet = currentPalletState.value.copy(
            zdjecieDuzejEtykietyUri = if (imageType == "PhotoLabel") pathString else currentPalletState.value.zdjecieDuzejEtykietyUri,
            zdjecieDuzejEtykietyUploadStatus = if (imageType == "PhotoLabel") UploadStatus.UPLOADING else currentPalletState.value.zdjecieDuzejEtykietyUploadStatus,

            zdjecieCalejPaletyUri = if (imageType == "PhotosOverview") pathString else currentPalletState.value.zdjecieCalejPaletyUri,
            zdjecieCalejPaletyUploadStatus = if (imageType == "PhotosOverview") UploadStatus.UPLOADING else currentPalletState.value.zdjecieCalejPaletyUploadStatus,

            damageInstances = when (imageType) {
                "PhotosDamage" -> {
                    if (damageInstanceId != null) {
                        // Ponawianie - znajdź i zaktualizuj status
                        newInstanceId = damageInstanceId // Użyj istniejącego ID
                        currentPalletState.value.damageInstances.map {
                            if (it.id == damageInstanceId) it.copy(uploadStatus = UploadStatus.UPLOADING) else it
                        }.toMutableList()
                    } else {
                        // Nowe zdjęcie - dodaj nową instancję
                        val newInstance = DamageInstance(photoUri = pathString, uploadStatus = UploadStatus.UPLOADING)
                        newInstanceId = newInstance.id // Zapisz ID nowej instancji
                        (currentPalletState.value.damageInstances + newInstance).toMutableList()
                    }
                }
                else -> currentPalletState.value.damageInstances // Bez zmian
            }
        )
        savedStateHandle["currentPallet"] = updatedPallet
        return updatedPallet to newInstanceId
    }

    /** Helper do `uploadPhotoInBackground`: Aktualizuje stan o wynik (SUCCESS/FAILED). */
    private fun updatePalletStateForUploadResult(
        result: Result<ImageUploadResponse>,
        imageType: String,
        damageInstanceId: String? // Musi być ID instancji (nowe lub stare)
    ) {
        val newStatus = if (result.isSuccess) UploadStatus.SUCCESS else UploadStatus.FAILED
        val fileId = result.getOrNull()?.fileId

        val updatedPallet = currentPalletState.value.copy(
            zdjecieDuzejEtykietyUploadStatus = if (imageType == "PhotoLabel") newStatus else currentPalletState.value.zdjecieDuzejEtykietyUploadStatus,
            zdjecieDuzejEtykietyFileId = if (imageType == "PhotoLabel" && fileId != null) fileId else currentPalletState.value.zdjecieDuzejEtykietyFileId,

            zdjecieCalejPaletyUploadStatus = if (imageType == "PhotosOverview") newStatus else currentPalletState.value.zdjecieCalejPaletyUploadStatus,
            zdjecieCalejPaletyFileId = if (imageType == "PhotosOverview" && fileId != null) fileId else currentPalletState.value.zdjecieCalejPaletyFileId,

            damageInstances = if (imageType == "PhotosDamage" && damageInstanceId != null) {
                currentPalletState.value.damageInstances.map {
                    if (it.id == damageInstanceId) {
                        it.copy(
                            uploadStatus = newStatus,
                            serverFileId = if (fileId != null) fileId else it.serverFileId // Zapisz fileId po sukcesie
                        )
                    } else it
                }.toMutableList()
            } else currentPalletState.value.damageInstances
        )
        savedStateHandle["currentPallet"] = updatedPallet
    }

    /** Aktualizuje status wszystkich zdjęć (np. przy błędzie pobierania indeksu) */
    private fun updateAllPhotoStatuses(status: UploadStatus) {
        val updatedPallet = currentPalletState.value.copy(
            zdjecieDuzejEtykietyUploadStatus = if (currentPalletState.value.zdjecieDuzejEtykietyUri != null) status else UploadStatus.IDLE,
            zdjecieCalejPaletyUploadStatus = if (currentPalletState.value.zdjecieCalejPaletyUri != null) status else UploadStatus.IDLE,
            damageInstances = currentPalletState.value.damageInstances.map { it.copy(uploadStatus = status) }.toMutableList()
        )
        savedStateHandle["currentPallet"] = updatedPallet
    }


    /**
     * Krok 3 & 4: Wywoływane z PalletEntryScreen.
     * Weryfikuje wysłane zdjęcia, ponawia nieudane i wysyła dane tekstowe.
     */
    fun saveAndPrepareNextPallet(isFormValid: Boolean) {
        viewModelScope.launch {
            val success = finalizePalletData(isFormValid)
            if (success) {
                resetCurrentPallet() // Wyczyść formularz
            }
        }
    }

    /** * NOWA FUNKCJA: Wywoływana przez przycisk "Zapisz i zakończ".
     * Robi to samo co `saveAndPrepareNextPallet`, ale nie czyści formularza, tylko nawiguje.
     */
    fun saveAndExitToSummary(isFormValid: Boolean) {
        viewModelScope.launch {
            val success = finalizePalletData(isFormValid)
            if (success) {
                startBackgroundPdfGeneration()
                _navigateToSummary.send(Unit)
            }
        }
    }

    /**
     * Wspólna logika dla obu przycisków "Zapisz".
     * Weryfikuje zdjęcia, ponawia nieudane, wysyła JSON.
     * Zwraca `true` przy sukcesie, `false` przy błędzie.
     */
    private suspend fun finalizePalletData(isFormValid: Boolean): Boolean {
        if (!isFormValid) {
            _submissionState.value = SubmissionState.Error("Uzupełnij wszystkie wymagane pola palety.")
            return false
        }

        val sessionId = currentSessionId.value
        val pallet = currentPalletState.value

        if (sessionId == null) {
            _submissionState.value = SubmissionState.Error("Błąd krytyczny: Brak ID sesji.")
            return false
        }

        // Spróbuj pobrać indeks, jeśli jeszcze go nie ma (np. użytkownik nic nie zeskanował, a klika zapisz)
        val palletIndex = getOrFetchPalletIndex()
        if (palletIndex == null) {
            _submissionState.value = SubmissionState.Error("Błąd sieci: Nie można pobrać indeksu palety.")
            return false
        }

        _submissionState.value = SubmissionState.InProgress("Weryfikowanie wysłanych zdjęć...")

        // Krok 1: Sprawdź, czy wszystkie zdjęcia są wysłane
        val failedPhotos = mutableListOf<Triple<String, String, String?>>() // imageType, path, damageInstanceId
        val currentPalletSnapshot = currentPalletState.value // Użyj migawki stanu

        if (currentPalletSnapshot.zdjecieDuzejEtykietyUri != null && currentPalletSnapshot.zdjecieDuzejEtykietyUploadStatus == UploadStatus.FAILED) {
            failedPhotos.add(Triple("PhotoLabel", currentPalletSnapshot.zdjecieDuzejEtykietyUri!!, null))
        }
        if (currentPalletSnapshot.zdjecieCalejPaletyUri != null && currentPalletSnapshot.zdjecieCalejPaletyUploadStatus == UploadStatus.FAILED) {
            failedPhotos.add(Triple("PhotosOverview", currentPalletSnapshot.zdjecieCalejPaletyUri!!, null))
        }
        currentPalletSnapshot.damageInstances.forEach {
            if (it.uploadStatus == UploadStatus.FAILED) {
                failedPhotos.add(Triple("PhotosDamage", it.photoUri, it.id))
            }
        }

        // Krok 2: Jeśli są niewysłane zdjęcia, ponów je TERAZ (blokująco)
        if (failedPhotos.isNotEmpty()) {
            _submissionState.value = SubmissionState.InProgress("Ponawianie wysyłania ${failedPhotos.size} zdjęć...")

            // ===== POCZĄTEK POPRAWKI (async/awaitAll) =====
            // Używamy coroutineScope, aby 'async' był dostępny
            val results = coroutineScope {
                val retryJobs = failedPhotos.map { (imageType, pathString, damageInstanceId) ->
                    // 'async' jest teraz wywoływany wewnątrz 'coroutineScope'
                    async(Dispatchers.IO) {
                        // Zaktualizuj UI na UPLOADING
                        val instanceIdToUpdate = if (imageType == "PhotosDamage") damageInstanceId else null
                        updatePalletStateForUploadStart(imageType, pathString, instanceIdToUpdate)

                        // Wywołaj API
                        val result = uploadImageHelper(
                            sessionId = sessionId,
                            palletIndex = palletIndex,
                            imageType = imageType,
                            pathString = pathString,
                            existingFileId = getExistingFileId(currentPalletSnapshot, imageType, damageInstanceId)
                        )

                        // Zaktualizuj stan o wynik
                        updatePalletStateForUploadResult(result, imageType, instanceIdToUpdate)
                        result.isSuccess
                    }
                }
                retryJobs.awaitAll() // Czekamy na wszystkie
            }

            // Sprawdzamy, czy 'any' z nich to 'false' (czyli 'it' == false)
            if (results.any { !it }) {
                // ===== KONIEC POPRAWKI (async/awaitAll) =====
                _submissionState.value = SubmissionState.Error("Nie udało się wysłać wszystkich zdjęć. Sprawdź sieć i spróbuj ponownie.")
                return false
            }
        }

        // Krok 3: Wszystkie zdjęcia wysłane, wysyłamy dane tekstowe JSON
        _submissionState.value = SubmissionState.InProgress("Wysyłanie danych palety ${palletIndex + 1}...")

        val finalPalletState = currentPalletState.value
        val result = sendPalletDataBlocking(sessionId, finalPalletState, palletIndex)

        return if (result.isSuccess) {
            saveCurrentPalletToList(result.getOrThrow())

            hdmLogger.log(application, "Pomyślnie wysłano paletę z indeksem: $palletIndex")
            Toast.makeText(application, "Zapisano paletę ${palletIndex + 1}", Toast.LENGTH_SHORT).show()
            _submissionState.value = SubmissionState.Idle
            true
        } else {
            hdmLogger.log(application, "Błąd wysyłania palety z indeksem: $palletIndex", LogLevel.ERROR)
            _submissionState.value = SubmissionState.Error("Błąd zapisu palety ${palletIndex + 1}. Spróbuj ponownie.")
            false
        }
    }


    /**
     * Ta funkcja jest teraz odpowiedzialna TYLKO za wysłanie danych tekstowych (JSON).
     * Zakłada, że zdjęcia są już wysłane i ich ID są w obiekcie `pallet`.
     */
    private suspend fun sendPalletDataBlocking(sessionId: String, pallet: DamagedPallet, index: Int): Result<DamagedPallet> {

        try {
            _submissionState.value = SubmissionState.InProgress("Wysyłanie danych tekstowych...")

            val textData = mutableMapOf<String, String>()
            val header = reportHeaderState.value
            val palletType = header.rodzajPalet

            textData["pallets_data.$index.pallet_number_raw"] = pallet.numerPalety
            if (palletType == "Surowiec" || palletType == "Inne") {
                pallet.numerLotu?.let { textData["pallets_data.$index.lot_number"] = it }
                pallet.rodzajTowaru?.let { textData["pallets_data.$index.item_symbol"] = it }
            }

            val damageKey = DescriptionGenerator.generateEnglishDamageKey(pallet)
            textData["pallets_data.$index.damage_type_key"] = damageKey

            val details = translationService.let {
                DescriptionGenerator.generateEnglishDetails(pallet, palletType, it, header.reportType)
            }
            textData["pallets_data.$index.details"] = details

            val damageSize = DescriptionGenerator.generateDamageSizeString(pallet)
            textData["pallets_data.$index.damage_size"] = damageSize

            val reportMode = when (palletType) {
                "Surowiec" -> "RM"
                "Wyrób gotowy" -> "FP"
                "Inne" -> "INNE"
                else -> ""
            }
            textData["pallets_data.$index.report_mode_at_save"] = reportMode

            textData["pallets_data.$index.full_pallet_model_json"] = json.encodeToString(pallet)

            val textResult = directSessionApiService.updateSession(sessionId, textData)
            if (textResult.isFailure) {
                Log.e(TAG, "Błąd wysyłania danych tekstowych palety $index: ${textResult.exceptionOrNull()?.message}")
                return Result.failure(textResult.exceptionOrNull()!!)
            }

            return Result.success(pallet)
        } catch (e: Exception) {
            Log.e(TAG, "Krytyczny błąd podczas sendPalletDataBlocking dla indeksu $index", e)
            return Result.failure(e)
        }
    }

    private suspend fun uploadImageHelper(
        sessionId: String,
        palletIndex: Int,
        imageType: String,
        pathString: String,
        existingFileId: String?
    ): Result<ImageUploadResponse> {
        val file = try {
            File(pathString)
        } catch (e: Exception) {
            Log.e(TAG, "Nie można uzyskać pliku ze ścieżki: $pathString", e)
            return Result.failure(e)
        }

        if (file == null || !file.exists()) {
            val errorMsg = "Plik obrazu nie istnieje: $pathString"
            Log.w(TAG, errorMsg)
            return Result.failure(Exception(errorMsg))
        }

        val result = directSessionApiService.uploadImage(
            sessionId = sessionId,
            palletIndex = palletIndex,
            imageType = imageType,
            file = file,
            existingFileId = existingFileId
        )

        return if (result.isFailure) {
            Log.w(TAG, "Nie udało się wysłać zdjęcia $imageType dla palety $palletIndex: ${result.exceptionOrNull()?.message}")
            Result.failure(result.exceptionOrNull()!!)
        } else {
            Log.i(TAG, "Pomyślnie wysłano zdjęcie $imageType dla palety $palletIndex. FileID: ${result.getOrNull()?.fileId}")
            result
        }
    }

    private suspend fun smartSyncLocalState(sessionId: String): Result<SyncReport> {
        Log.i(TAG, "Rozpoczynam inteligentną synchronizację (smartSyncLocalState) dla $sessionId...")
        _submissionState.value = SubmissionState.Generating(ProcessingStep.VERIFYING_DATA, 0.7f)

        val summaryResult = directSessionApiService.getSessionSummary(sessionId)
        if (summaryResult.isFailure) {
            Log.e(TAG, "smartSync: Nie można pobrać podsumowania sesji: ${summaryResult.exceptionOrNull()?.message}")
            return Result.failure(summaryResult.exceptionOrNull() ?: Exception("Błąd pobierania podsumowania"))
        }

        val serverSummary = summaryResult.getOrThrow()
        Log.d(TAG, "smartSync: Serwer ma ${serverSummary.palletCount} palet")

        val syncReport = SyncReport()

        val headerDataMap = buildHeaderDataMap()
        val headerResult = directSessionApiService.updateSession(sessionId, headerDataMap)
        if (headerResult.isFailure) {
            Log.e(TAG, "smartSync: Błąd synchronizacji nagłówka: ${headerResult.exceptionOrNull()?.message}")
            return Result.failure(headerResult.exceptionOrNull() ?: Exception("Błąd synchronizacji nagłówka"))
        }
        syncReport.headerSynced = true
        Log.d(TAG, "smartSync: Nagłówek zsynchronizowany ✓")
        _submissionState.value = SubmissionState.Generating(ProcessingStep.VERIFYING_DATA, 0.73f)

        val localPallets = savedPallets.value
        val serverPalletIndices = serverSummary.palletsSummary.map { it.index }.toSet()

        Log.d(TAG, "smartSync: Lokalne palety: ${localPallets.size}, Serwer ma indeksy: $serverPalletIndices")

        var palletIdx = 0
        for (pallet in localPallets) {
            if (pallet.serverIndex == null) {
                Log.e(TAG, "smartSync: KRYTYCZNY błąd - paleta ${pallet.id} nie ma serverIndex!")
                syncReport.errors.add("Paleta ${pallet.numerPalety} nie ma przypisanego indeksu")
                continue
            }

            val needsSync = pallet.serverIndex !in serverPalletIndices

            if (needsSync) {
                Log.d(TAG, "smartSync: Paleta index=${pallet.serverIndex} BRAKUJE na serwerze, wysyłam...")
                val palletResult = sendPalletDataBlocking(sessionId, pallet, pallet.serverIndex!!)

                if (palletResult.isFailure) {
                    Log.e(TAG, "smartSync: Błąd synchronizacji palety index=${pallet.serverIndex}")
                    syncReport.errors.add("Nie można wysłać palety ${pallet.numerPalety}")
                    return Result.failure(palletResult.exceptionOrNull() ?: Exception("Błąd synchronizacji palety"))
                }
                syncReport.palletsSynced++
            } else {
                Log.d(TAG, "smartSync: Paleta index=${pallet.serverIndex} już istnieje na serwerze, pomijam ✓")
                syncReport.palletsSkipped++
            }

            palletIdx++
            val currentProgress = 0.73f + (0.12f * (palletIdx.toFloat() / localPallets.size.toFloat()))
            _submissionState.value = SubmissionState.Generating(ProcessingStep.VERIFYING_DATA, currentProgress)
        }

        val localIndices = localPallets.mapNotNull { it.serverIndex }.toSet()
        val extraPalletsOnServer = serverPalletIndices - localIndices
        if (extraPalletsOnServer.isNotEmpty()) {
            Log.w(TAG, "smartSync: UWAGA - Serwer ma palety których nie ma lokalnie: $extraPalletsOnServer")
            syncReport.warnings.add("Serwer ma ${extraPalletsOnServer.size} palet więcej niż lokalnie")
        }

        _submissionState.value = SubmissionState.Generating(ProcessingStep.VERIFYING_DATA, 0.85f)

        val positionsResult = directSessionApiService.updateSession(sessionId, mapOf("pallet_positions_json" to json.encodeToString(palletPositions.value)))
        if (positionsResult.isFailure) {
            Log.e(TAG, "smartSync: Błąd synchronizacji pozycji palet")
            return Result.failure(positionsResult.exceptionOrNull() ?: Exception("Błąd synchronizacji pozycji"))
        }
        syncReport.layoutSynced = true
        Log.d(TAG, "smartSync: Pozycje palet zsynchronizowane ✓")

        val markersResult = directSessionApiService.updateSession(sessionId, mapOf("damage_markers_json" to json.encodeToString(damageMarkersState.value)))
        if (markersResult.isFailure) {
            Log.e(TAG, "smartSync: Błąd synchronizacji markerów")
            return Result.failure(markersResult.exceptionOrNull() ?: Exception("Błąd synchronizacji markerów"))
        }
        Log.d(TAG, "smartSync: Markery zsynchronizowane ✓")

        val heightsResult = directSessionApiService.updateSession(sessionId, mapOf("damage_heights_json" to json.encodeToString(damageHeightSelections.value)))
        if (heightsResult.isFailure) {
            Log.e(TAG, "smartSync: Błąd synchronizacji wysokości")
            return Result.failure(heightsResult.exceptionOrNull() ?: Exception("Błąd synchronizacji wysokości"))
        }
        Log.d(TAG, "smartSync: Wysokości zsynchronizowane ✓")

        _submissionState.value = SubmissionState.Generating(ProcessingStep.VERIFYING_DATA, 0.9f)

        Log.i(TAG, "smartSync: Synchronizacja zakończona ✓")
        Log.i(TAG, "  - Palet wysłanych: ${syncReport.palletsSynced}")
        Log.i(TAG, "  - Palet pominiętych: ${syncReport.palletsSkipped}")
        Log.i(TAG, "  - Nagłówek: zsynchronizowany")
        Log.i(TAG, "  - Layout: zsynchronizowany")

        return Result.success(syncReport)
    }

    fun startBackgroundPdfGeneration() {
        if (_backgroundPdfState.value is BackgroundPdfState.Generating) {
            Log.d(TAG, "PDF już się generuje w tle, pomijam")
            return
        }

        if (_backgroundPdfState.value is BackgroundPdfState.Ready) {
            Log.d(TAG, "PDF już są gotowe, pomijam")
            return
        }

        val sessionId = currentSessionId.value
        if (sessionId == null) {
            Log.w(TAG, "Brak sessionId, nie mogę rozpocząć generowania PDF w tle")
            return
        }

        val isReady = if (isNagoyaFlow.value) {
            isNagoyaReportReadyForGeneration()
        } else {
            isReportReadyForGeneration()
        }

        if (!isReady) {
            Log.d(TAG, "Raport nie jest gotowy, pomijam generowanie PDF w tle")
            return
        }

        backgroundPdfJob?.cancel()
        backgroundPdfJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _backgroundPdfState.value = BackgroundPdfState.Generating
                Log.i(TAG, "⏳ Rozpoczynam generowanie PDF w tle...")

                val pdfPlFile = File(application.cacheDir, "background_PL_${sessionId}.pdf")
                val pdfEnFile = File(application.cacheDir, "background_EN_${sessionId}.pdf")

                Log.d(TAG, "Generuję PDF (PL) w tle...")
                pdfPlFile.outputStream().use { stream ->
                    val result = generatePdfToStream(
                        application,
                        PdfReportGenerator.Language.PL,
                        strategy.value!!.getPdfConfig(),
                        stream
                    )
                    if (result.isFailure) {
                        throw result.exceptionOrNull() ?: Exception("Błąd generowania PDF PL")
                    }
                }

                Log.d(TAG, "Generuję PDF (EN) w tle...")
                pdfEnFile.outputStream().use { stream ->
                    val result = generatePdfToStream(
                        application,
                        PdfReportGenerator.Language.EN,
                        strategy.value!!.getPdfConfig(),
                        stream
                    )
                    if (result.isFailure) {
                        throw result.exceptionOrNull() ?: Exception("Błąd generowania PDF EN")
                    }
                }

                _backgroundPdfState.value = BackgroundPdfState.Ready(
                    pdfPlPath = pdfPlFile.absolutePath,
                    pdfEnPath = pdfEnFile.absolutePath
                )
                Log.i(TAG, "✅ PDF wygenerowane w tle pomyślnie!")
                hdmLogger.log(application, "PDF wygenerowane w tle i gotowe do wysłania")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Błąd generowania PDF w tle", e)
                _backgroundPdfState.value = BackgroundPdfState.Error(e.message ?: "Nieznany błąd")
            }
        }
    }

    private fun cleanupBackgroundPdfs() {
        val state = _backgroundPdfState.value
        if (state is BackgroundPdfState.Ready) {
            try {
                File(state.pdfPlPath).delete()
                File(state.pdfEnPath).delete()
                Log.d(TAG, "Wyczyszczono PDF z cache")
            } catch (e: Exception) {
                Log.w(TAG, "Błąd czyszczenia PDF z cache", e)
            }
        }
        _backgroundPdfState.value = BackgroundPdfState.Idle
    }

    fun startFullReportUpload() {
        val sessionId = currentSessionId.value
        if (sessionId == null) {
            _submissionState.value = SubmissionState.Error("Błąd krytyczny: Brak ID sesji.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _submissionState.value = SubmissionState.Generating(ProcessingStep.SENDING_COMMENTS, 0.05f)
                val comments = reportHeaderState.value.opisZdarzenia
                if (comments.isNotBlank()) {
                    val commentsResult = directSessionApiService.updateSession(
                        sessionId,
                        mapOf("header_data.comments" to comments)
                    )
                    if(commentsResult.isFailure) {
                        _submissionState.value = SubmissionState.Error(
                            "Błąd wysyłania opisu: ${commentsResult.exceptionOrNull()?.message}"
                        )
                        return@launch
                    }
                }

                val pdfState = _backgroundPdfState.value

                when (pdfState) {
                    is BackgroundPdfState.Ready -> {
                        Log.i(TAG, "Używam PDF wygenerowanych w tle ✓")
                        _submissionState.value = SubmissionState.Generating(ProcessingStep.UPLOADING_PDF, 0.15f)

                        val pdfPlFile = File(pdfState.pdfPlPath)
                        val uploadPlResult = directSessionApiService.uploadPdf(sessionId, "PL", pdfPlFile)
                        if (uploadPlResult.isFailure) {
                            _submissionState.value = SubmissionState.Error(
                                "Błąd wysyłki PDF (PL): ${uploadPlResult.exceptionOrNull()?.message}"
                            )
                            return@launch
                        }

                        _submissionState.value = SubmissionState.Generating(ProcessingStep.UPLOADING_PDF, 0.35f)

                        val pdfEnFile = File(pdfState.pdfEnPath)
                        val uploadEnResult = directSessionApiService.uploadPdf(sessionId, "EN", pdfEnFile)
                        if (uploadEnResult.isFailure) {
                            _submissionState.value = SubmissionState.Error(
                                "Błąd wysyłki PDF (EN): ${uploadEnResult.exceptionOrNull()?.message}"
                            )
                            return@launch
                        }

                        hdmLogger.log(application, "PDF wysłane z cache (wygenerowane w tle)")
                    }

                    is BackgroundPdfState.Generating -> {
                        Log.i(TAG, "PDF się jeszcze generują w tle, czekam...")
                        _submissionState.value = SubmissionState.Generating(ProcessingStep.GENERATING_PDF, 0.15f)

                        var waitTime = 0L
                        while (_backgroundPdfState.value is BackgroundPdfState.Generating && waitTime < 30000) {
                            delay(500)
                            waitTime += 500
                        }

                        if (_backgroundPdfState.value is BackgroundPdfState.Ready) {
                            val readyState = _backgroundPdfState.value as BackgroundPdfState.Ready
                            _submissionState.value = SubmissionState.Generating(ProcessingStep.UPLOADING_PDF, 0.25f)

                            val uploadPlResult = directSessionApiService.uploadPdf(
                                sessionId, "PL", File(readyState.pdfPlPath)
                            )
                            if (uploadPlResult.isFailure) throw Exception("Błąd wysyłki PDF (PL)")

                            val uploadEnResult = directSessionApiService.uploadPdf(
                                sessionId, "EN", File(readyState.pdfEnPath)
                            )
                            if (uploadEnResult.isFailure) throw Exception("Błąd wysyłki PDF (EN)")

                            Log.i(TAG, "PDF wygenerowane w tle i wysłane ✓")
                        } else {
                            Log.w(TAG, "Timeout generowania w tle, generuję teraz...")
                            throw Exception("Przekroczono czas oczekiwania na PDF")
                        }
                    }

                    else -> {
                        Log.i(TAG, "PDF nie są gotowe, generuję teraz...")
                        _submissionState.value = SubmissionState.Generating(ProcessingStep.GENERATING_PDF, 0.15f)

                        val pdfPlResult = generateAndUploadPdf(
                            application, sessionId, PdfReportGenerator.Language.PL, "PL"
                        )
                        if (pdfPlResult.isFailure) {
                            _submissionState.value = SubmissionState.Error(
                                "Błąd generowania PDF (PL): ${pdfPlResult.exceptionOrNull()?.message}"
                            )
                            return@launch
                        }

                        _submissionState.value = SubmissionState.Generating(ProcessingStep.UPLOADING_PDF, 0.35f)

                        val pdfEnResult = generateAndUploadPdf(
                            application, sessionId, PdfReportGenerator.Language.EN, "EN"
                        )
                        if (pdfEnResult.isFailure) {
                            _submissionState.value = SubmissionState.Error(
                                "Błąd generowania PDF (EN): ${pdfEnResult.exceptionOrNull()?.message}"
                            )
                            return@launch
                        }
                    }
                }

                _submissionState.value = SubmissionState.Generating(ProcessingStep.VERIFYING_DATA, 0.7f)
                val syncResult = smartSyncLocalState(sessionId)

                if (syncResult.isFailure) {
                    _submissionState.value = SubmissionState.Error(
                        "Błąd synchronizacji: ${syncResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val syncReport = syncResult.getOrThrow()
                Log.i(TAG, "Synchronizacja: ${syncReport.palletsSynced} wysłanych, ${syncReport.palletsSkipped} pominiętych")

                _submissionState.value = SubmissionState.Generating(ProcessingStep.FINALIZING, 0.95f)
                val finalizeResult = directSessionApiService.finalizeSession(sessionId)

                if (finalizeResult.isSuccess) {
                    withContext(Dispatchers.Main) {
                        _submissionState.value = SubmissionState.FinalizeSuccess
                    }

                    val message = buildString {
                        append("Sesja sfinalizowana pomyślnie. ")
                        append("Palet: ${syncReport.palletsSynced} wysłanych, ${syncReport.palletsSkipped} pominiętych.")
                    }
                    hdmLogger.log(application, message)

                    archiveSession(sessionId)
                    delay(2000)

                    withContext(Dispatchers.Main) {
                        fullReset()
                    }
                    UserManager.logoutUser()
                    _logoutAndNavigateToLogin.send(Unit)
                } else {
                    _submissionState.value = SubmissionState.Error(
                        "Błąd finalizacji: ${finalizeResult.exceptionOrNull()?.message}"
                    )
                }

            } finally {
                cleanupBackgroundPdfs()
            }
        }
    }

    private suspend fun archiveSession(sessionId: String) {
        try {
            Log.d(TAG, "Rozpoczynam archiwizację sesji $sessionId")
            val archived = ArchivedSession(
                archiveId = "ARCHIVE_${System.currentTimeMillis()}",
                originalSessionId = sessionId,
                reportType = reportHeaderState.value.reportType,
                completionTimestamp = System.currentTimeMillis(),
                reportHeader = reportHeaderState.value,
                savedPallets = savedPallets.value,
                palletPositions = palletPositions.value,
                damageMarkers = damageMarkersState.value,
                damageHeightSelections = damageHeightSelections.value,
                palletCount = savedPallets.value.size
            )
            ArchiveRepository.archiveSession(application, archived)
            Log.d(TAG, "Archiwizacja sesji $sessionId zakończona pomyślnie.")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas archiwizacji sesji $sessionId", e)
            hdmLogger.log(application, "Błąd archiwizacji sesji $sessionId: ${e.message}", LogLevel.ERROR)
        }
    }

    private suspend fun generateAndUploadPdf(context: Context, sessionId: String, language: PdfReportGenerator.Language, tag: String): Result<Unit> {
        val pdfFile = File(context.cacheDir, "temp_${tag}_${sessionId}.pdf")
        try {
            val genResult = pdfFile.outputStream().use { stream ->
                generatePdfToStream(context, language, strategy.value!!.getPdfConfig(), stream)
            }
            if (genResult.isFailure) return genResult

            val uploadResult = directSessionApiService.uploadPdf(sessionId, tag, pdfFile)
            if (uploadResult.isFailure) return Result.failure(uploadResult.exceptionOrNull()!!)

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            pdfFile.delete()
        }
    }


    fun loadPendingSessions() {
        viewModelScope.launch {
            _isSessionLoading.value = true
            val result = directSessionApiService.getPendingSessions()
            result.onSuccess { response ->
                _pendingSessions.value = response.sessions.sortedByDescending { it.headerData?.reportDatetime }
            }.onFailure {
                Toast.makeText(application, "Błąd pobierania sesji: ${it.message}", Toast.LENGTH_SHORT).show()
                _pendingSessions.value = emptyList()
            }
            _isSessionLoading.value = false
        }
    }

    fun loadSession(sessionId: String, onSessionLoaded: (Screen) -> Unit) {
        viewModelScope.launch {
            _isSessionLoading.value = true
            val result = directSessionApiService.getSessionDetails(sessionId)

            result.onSuccess { details ->
                val header = details.headerData
                val pallets = details.palletsData

                if (header == null) {
                    Toast.makeText(application, "Błąd: Sesja nie zawiera danych nagłówka.", Toast.LENGTH_SHORT).show()
                    _isSessionLoading.value = false
                    return@launch
                }

                val reportType = header.reportType ?: "Rozładunek dostawy"
                _strategy.value = ReportStrategyFactory.create(reportType)
                savedStateHandle["sessionId"] = sessionId
                hdmLogger.setSessionId(sessionId)

                val restoredHeader = ReportHeader(
                    reportType = reportType,
                    magazynier = header.picWh ?: "",
                    workerId = UserManager.getLoggedInUser()?.workerId,
                    miejsce = header.place ?: "",
                    lokalizacja = header.warehouseLocation ?: "",
                    rodzajWozka = header.forklift_type ?: "",
                    dataGodzina = header.report_timestamp_long ?: System.currentTimeMillis(),
                    rodzajPalet = when(header.reportModeAtSave) {
                        "RM" -> "Surowiec"
                        "FP" -> "Wyrób gotowy"
                        "INNE" -> "Inne"
                        else -> ""
                    },

                    // ===== POPRAWKA (Z POPRZEDNIEJ ODPOWIEDZI) =====
                    rodzajSamochodu = header.vehicle_type ?: (header.truckNumber ?: (if(header.truckNumberCustom != null) "Inne" else "")),
                    // ===============================================

                    numerAuta = header.truckNumberCustom?.split("/")?.getOrNull(0) ?: "",
                    numerNaczepyKontenera = header.truckNumberCustom?.split("/")?.getOrNull(1) ?: "",
                    numerDokumentuFeniks = header.feniksRefNo ?: "",
                    opisZdarzenia = header.comments ?: "",
                    numerKontenera = header.containerNumber ?: ""
                )
                savedStateHandle["reportHeader"] = restoredHeader

                // ===== POCZĄTEK POPRAWKI (BŁĄD KOMPILACJI) =====
                // Usunięto: val attachmentsMap = details.attachments_base64 ?: emptyMap()
                // attachments_base64 jest teraz wewnątrz `palletData`

                val restoredPallets = pallets?.mapIndexedNotNull { index, palletData ->
                    if (palletData.full_pallet_model_json != null) {
                        try {
                            json.decodeFromString<DamagedPallet>(palletData.full_pallet_model_json).copy(
                                serverIndex = index
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd deserializacji full_pallet_model_json dla indeksu $index", e)
                            createPartialPallet(palletData, index) // <-- Usunięto 'attachmentsMap'
                        }
                    } else {
                        createPartialPallet(palletData, index) // <-- Usunięto 'attachmentsMap'
                    }
                } ?: emptyList()
                // ===== KONIEC POPRAWKI (BŁĄD KOMPILACJI) =====

                savedStateHandle["savedPallets"] = restoredPallets

                try {
                    var restoredPositions: List<PalletPosition> = details.pallet_positions_json?.let {
                        json.decodeFromString(it)
                    } ?: emptyList()

                    // ===== POCZĄTEK POPRAWKI (URI) =====
                    // Synchronizujemy trwałe URI z palet do tymczasowych pól w pozycjach
                    if (restoredPositions.isNotEmpty() && restoredPallets.isNotEmpty()) {
                        val palletUriMap = restoredPallets.associate { it.id to it.damageMarkingBitmapUri }
                        restoredPositions = restoredPositions.map { position ->
                            val persistentUri = palletUriMap[position.palletId]
                            if (persistentUri != null && position.damageBitmapUri == null) {
                                // Kopiujemy trwałe URI do pola @Transient
                                position.copy(damageBitmapUri = persistentUri)
                            } else {
                                position
                            }
                        }
                    }
                    // ===== KONIEC POPRAWKI (URI) =====

                    savedStateHandle["palletPositions"] = restoredPositions // Zapisujemy poprawioną listę

                    val restoredMarkers: Map<String, List<DamageMarker>> = details.damage_markers_json?.let {
                        json.decodeFromString(it)
                    } ?: emptyMap()
                    savedStateHandle["damageMarkers"] = restoredMarkers

                    val restoredHeights: Map<String, Map<String, Set<String>>> = details.damage_heights_json?.let {
                        json.decodeFromString(it)
                    } ?: emptyMap()
                    savedStateHandle["damageHeightSelections"] = restoredHeights
                } catch (e: Exception) {
                    Log.e(TAG, "Błąd deserializacji dodatkowego stanu sesji (positions/markers/heights)", e)
                    savedStateHandle["palletPositions"] = emptyList<PalletPosition>()
                    savedStateHandle["damageMarkers"] = emptyMap<String, List<DamageMarker>>()
                    savedStateHandle["damageHeightSelections"] = emptyMap<String, Map<String, Set<String>>>()
                }

                // ===== POCZĄTEK NOWEJ POPRAWKI (WCZYTANIE LAYOUTU) =====
                // Wczytaj zapisany układ naczepy z serwera
                val restoredLayout = details.selected_vehicle_layout
                if (restoredLayout != null) {
                    savedStateHandle["selectedVehicleLayout"] = restoredLayout
                    Log.d(TAG, "Wczytano zapisany układ naczepy: $restoredLayout")
                } else {
                    // Jeśli na serwerze nie ma (starsza sesja), wyczyść lokalny stan
                    savedStateHandle["selectedVehicleLayout"] = null
                }
                // ===== KONIEC NOWEJ POPRAWKI (WCZYTANIE LAYOUTU) =====

                // === POPRAWKA Z POPRZEDNIEJ ODPOWIEDZI (REGENERACJA BITMAP) ===
                withContext(Dispatchers.IO) {
                    regenerateDamageBitmapsAfterSessionLoad()
                }

                hdmLogger.log(application, "Wznowiono pracę nad sesją (online): $sessionId")
                _isSessionLoading.value = false

                onSessionLoaded(Screen.Header)
                // === KONIEC POPRAWKI (REGENERACJA BITMAP) ===

            }.onFailure {
                Toast.makeText(application, "Błąd wczytania sesji: ${it.message}", Toast.LENGTH_SHORT).show()
                _isSessionLoading.value = false
            }
        }
    }



    private suspend fun generateBitmapWithMarkers(
        context: Context,
        baseImageUri: String, // Ten parametr jest ignorowany, ale zostawiamy dla sygnatury
        markers: List<DamageMarker>
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Wczytaj obraz bazowy ze statycznego zasobu
            val inputStream = context.resources.openRawResource(R.drawable.pallets) // <-- POPRAWKA
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
            inputStream.close()

            // Stwórz kopię do rysowania
            val mutableBitmap = originalBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            // Nałóż markery
            markers.forEachIndexed { index, marker ->
                val textPaint = Paint().apply {
                    color = marker.color.toArgb()
                    textSize = 50f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    style = Paint.Style.FILL_AND_STROKE
                    strokeWidth = 2f
                }
                val backgroundPaint = Paint().apply {
                    color = Color.White.copy(alpha = 0.8f).toArgb()
                    style = Paint.Style.FILL
                }

                val x = marker.coordinates.x * originalBitmap.width
                val y = marker.coordinates.y * originalBitmap.height

                canvas.drawCircle(x, y, 30f, backgroundPaint)
                canvas.drawText((index + 1).toString(), x, y - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint)
            }

            // Zapisz do pliku
            val fileName = "damage_locations_${System.currentTimeMillis()}.png"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                mutableBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }

            return@withContext file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas generowania bitmapy z markerami", e)
            return@withContext null
        }
    }

    /**
     * Regeneruje bitmapy uszkodzeń dla wszystkich PalletPosition po wczytaniu sesji.
     * Wywoływane automatycznie po loadSessionFromServer().
     */
    private suspend fun regenerateDamageBitmapsAfterSessionLoad() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "🔄 Rozpoczynam regenerację bitmap uszkodzeń po wczytaniu sesji...")

            val positions = palletPositions.value
            if (positions.isEmpty()) {
                Log.d(TAG, "Brak pozycji do regeneracji")
                return@withContext
            }

            positions.forEach { position ->
                val palletId = position.palletId
                val markers = damageMarkersState.value[palletId]


                if (!markers.isNullOrEmpty()) {
                    Log.d(TAG, "🎨 Regeneruję bitmapę dla palety $palletId (pozycja ${position.positionOnVehicle})")

                    val pallet = savedPallets.value.find { it.id == palletId }
                    val baseImageUri = pallet?.zdjecieCalejPaletyUri

                    if (baseImageUri != null) {
                        try {
                            // Generuj bitmapę z markerami
                            val savedUri = generateBitmapWithMarkers(
                                context = getApplication(),
                                baseImageUri = baseImageUri,
                                markers = markers
                            )

                            if (savedUri != null) {
                                // Zaktualizuj pozycję z nowym URI
                                val updatedPosition = position.copy(damageBitmapUri = savedUri)
                                val updatedPositions = positions.map { pos ->
                                    if (pos.palletId == palletId && pos.positionOnVehicle == position.positionOnVehicle) {
                                        updatedPosition
                                    } else {
                                        pos
                                    }
                                }
                                savedStateHandle["palletPositions"] = updatedPositions
                                Log.d(TAG, "✅ Bitmapa zregenerowana: $savedUri")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Błąd regeneracji bitmapy dla palety $palletId: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Brak zdjęcia całej palety dla $palletId - nie można zregenerować bitmapy")
                    }
                }
            }

            Log.d(TAG, "✅ Regeneracja bitmap zakończona")
        }
    }


    private fun findAndSaveAttachment(
        attachments: Map<String, List<Base64Attachment>>,
        fileId: String?,
        prefix: String
    ): String? {
        if (fileId == null) return null

        val attachmentData = attachments.values.flatten().find { it.fileId == fileId }?.dataBase64

        return if (attachmentData != null) {
            try {
                ImageUtils.saveBase64ToTempFile(application, attachmentData, prefix)
            } catch (e: Exception) {
                Log.e(TAG, "Nie udało się zapisać załącznika Base64 do pliku", e)
                null
            }
        } else {
            Log.w(TAG, "Nie znaleziono danych Base64 dla fileId: $fileId")
            null
        }
    }

    // ===== POCZĄTEK POPRAWKI (BŁĄD KOMPILACJI) =====
    private fun createPartialPallet(
        palletData: SessionPalletData,
        index: Int
    ): DamagedPallet {
        // Pobieramy mapę załączników BEZPOŚREDNIO z palletData
        val attachments = palletData.attachments_base64 ?: emptyMap()
        // ===== KONIEC POPRAWKI (BŁĄD KOMPILACJI) =====

        val labelFileId = palletData.photoLabel?.firstOrNull()
        val labelUri = findAndSaveAttachment(attachments, labelFileId, "label_${index}")

        val overviewFileId = palletData.photosOverview?.firstOrNull()
        val overviewUri = findAndSaveAttachment(attachments, overviewFileId, "overview_${index}")

        val damageInstances = palletData.photosDamage?.mapNotNull { fileId ->
            val damageUri = findAndSaveAttachment(attachments, fileId, "damage_${index}_${UUID.randomUUID().toString().take(4)}")
            if (damageUri != null) {
                DamageInstance(
                    photoUri = damageUri,
                    serverFileId = fileId,
                    details = mutableListOf()
                )
            } else {
                null
            }
        }?.toMutableList() ?: mutableListOf<DamageInstance>()

        return DamagedPallet(
            id = UUID.randomUUID().toString(),
            serverIndex = index,
            numerPalety = palletData.palletNumberRaw ?: "",
            rodzajTowaru = palletData.itemSymbol,
            numerLotu = palletData.lotNumber,
            damageInstances = damageInstances,
            zdjecieDuzejEtykietyUri = labelUri,
            zdjecieDuzejEtykietyFileId = labelFileId,
            zdjecieCalejPaletyUri = overviewUri,
            zdjecieCalejPaletyFileId = overviewFileId
        )
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            _isSessionLoading.value = true
            val result = directSessionApiService.deleteSession(sessionId)
            result.onSuccess {
                Toast.makeText(application, it.message, Toast.LENGTH_SHORT).show()
                loadPendingSessions()
            }.onFailure {
                Toast.makeText(application, "Błąd usuwania: ${it.message}", Toast.LENGTH_SHORT).show()
            }
            _isSessionLoading.value = false
        }
    }

    private fun updateSessionStateAsync(key: String, jsonValue: String) {
        val sessionId = currentSessionId.value
        if (sessionId == null) {
            Log.w(TAG, "Brak sessionId, pomijam updateSessionStateAsync dla $key")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = directSessionApiService.updateSession(sessionId, mapOf(key to jsonValue))
            if (result.isFailure) {
                Log.w(TAG, "Błąd synchronizacji stanu w tle dla $key: ${result.exceptionOrNull()?.message}")
                hdmLogger.log(application, "Błąd synchronizacji $key", LogLevel.WARNING)
            } else {
                Log.d(TAG, "Pomyślnie zsynchronizowano $key")
            }
        }
    }

    fun generateAndSetEventDescription() {
        if (savedPallets.value.isNotEmpty()) {
            val reportType = reportHeaderState.value.reportType
            if (reportType.isNotBlank()) {
                val description = DescriptionGenerator.generatePolishEventDescription(savedPallets.value, reportType)
                updateOpisZdarzenia(description)
            }
        } else {
            updateOpisZdarzenia("")
        }
    }

    fun fullReset() {
        _isNagoyaFlow.value = false
        _strategy.value = null
        savedStateHandle["reportHeader"] = ReportHeader()
        savedStateHandle["currentPallet"] = DamagedPallet()
        savedStateHandle["savedPallets"] = emptyList<DamagedPallet>()
        savedStateHandle["palletPositions"] = emptyList<PalletPosition>()
        savedStateHandle["selectedVehicleLayout"] = null
        savedStateHandle["damageMarkers"] = emptyMap<String, List<DamageMarker>>()
        savedStateHandle["damageHeightSelections"] = emptyMap<String, Map<String, Set<String>>>()
        savedStateHandle["sessionId"] = null
        savedStateHandle["currentScreenRoute"] = null
        _submissionState.value = SubmissionState.Idle
        hdmLogger.setSessionId(null)
        cleanupBackgroundPdfs()
        backgroundPdfJob?.cancel()
    }

    fun loadArchivedSession(archived: ArchivedSession) {
        fullReset()

        val newSessionId = DirectSessionApiService.generateSessionId()
        savedStateHandle["sessionId"] = newSessionId
        hdmLogger.setSessionId(newSessionId)

        val palletsWithUris = archived.savedPallets.map { pallet ->
            pallet.copy(
                zdjecieDuzejEtykietyUri = pallet.zdjecieDuzejEtykietyBase64?.let {
                    ImageUtils.saveBase64ToTempFile(application, it, "restored_label_${pallet.id}")
                },
                zdjecieCalejPaletyUri = pallet.zdjecieCalejPaletyBase64?.let {
                    ImageUtils.saveBase64ToTempFile(application, it, "restored_whole_${pallet.id}")
                },
                damageMarkingBitmapUri = pallet.damageMarkingBitmapBase64?.let {
                    ImageUtils.saveBase64ToTempFile(application, it, "restored_marking_${pallet.id}")
                },
                damageInstances = pallet.damageInstances.mapIndexedNotNull { idx, instance ->
                    val restoredPath = instance.photoBase64?.let {
                        ImageUtils.saveBase64ToTempFile(application, it, "restored_damage_${pallet.id}_$idx")
                    }

                    if (restoredPath != null) {
                        instance.copy(photoUri = restoredPath)
                    } else {
                        Log.w(TAG, "Pominięto przywracanie zdjęcia (brak Base64) dla palety ${pallet.id}")
                        null
                    }
                }.toMutableList()
            )
        }

        savedStateHandle["reportHeader"] = archived.reportHeader
        savedStateHandle["savedPallets"] = palletsWithUris
        savedStateHandle["palletPositions"] = archived.palletPositions
        savedStateHandle["selectedVehicleLayout"] = null
        savedStateHandle["damageMarkers"] = archived.damageMarkers
        savedStateHandle["damageHeightSelections"] = archived.damageHeightSelections

        _strategy.value = ReportStrategyFactory.create(archived.reportType)

        hdmLogger.log(
            application,
            "Wczytano zarchiwizowany raport: ${archived.reportType} jako nową sesję $newSessionId"
        )

        sendHeaderData()
    }

    private suspend fun generatePdfToStream(
        context: Context,
        language: PdfReportGenerator.Language,
        config: PdfReportGenerator.PdfConfig,
        outputStream: OutputStream
    ): Result<Unit> {
        val reportType = reportHeaderState.value.reportType
        if (reportType.isBlank()) {
            return Result.failure(IllegalStateException("Krytyczny błąd: Typ raportu w nagłówku jest pusty! Nie można wygenerować PDF."))
        }
        val activeStrategy = ReportStrategyFactory.create(reportType)

        return try {
            val isWyrobGotowy = reportHeaderState.value.rodzajPalet == "Wyrób gotowy"

            val (header, pallets, damageTranslationMap) = if (language == PdfReportGenerator.Language.EN) {
                translateReportData(reportHeaderState.value, savedPallets.value)
            } else {
                Triple(reportHeaderState.value, savedPallets.value, emptyMap())
            }

            val pdfTitle = activeStrategy.getPdfTitle()
            val translatedPdfTitle = if (language == PdfReportGenerator.Language.EN) {
                translationService.translate(pdfTitle)
            } else {
                pdfTitle
            }

            val headerWithoutSignature = header.copy(podpisOdbierajacegoUri = null)

            // === POCZĄTEK POPRAWKI ===
            // Ta sama logika co w funkcji powyżej
            val vehicleTypeFromHeader = reportHeaderState.value.rodzajSamochodu
            val layoutFromState = selectedVehicleLayoutState.value
            val isNaczepaVehicle = vehicleTypeFromHeader in listOf("Naczepa", "OKTRANS", "ROTONDO", "DSV")
            val isTandemVehicle = vehicleTypeFromHeader == "Tandem"
            val isSoloVehicle = vehicleTypeFromHeader == "Solo"
            val requiresLayoutSelection = isNaczepaVehicle || isTandemVehicle || isSoloVehicle

            val finalLayoutForPdf = if (requiresLayoutSelection) {
                layoutFromState
            } else {
                vehicleTypeFromHeader
            }
            // === KONIEC POPRAWKI ===

            pdfGenerator.generatePdfReport(
                outputStream = outputStream, context = context, reportHeader = headerWithoutSignature,
                savedPallets = pallets, palletPositions = palletPositions.value,
                // === ZMIANA PRZEKAZANIA PARAMETRU ===
                selectedVehicleLayout = finalLayoutForPdf,
                // ====================================
                config = config, damageMarkers = damageMarkersState.value,
                damageHeightSelections = damageHeightSelections.value, language = language,
                pdfTitle = translatedPdfTitle, isWyrobGotowyOverride = isWyrobGotowy,
                damageTranslationMap = damageTranslationMap
            ).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun translateReportData(
        header: ReportHeader,
        pallets: List<DamagedPallet>
    ): Triple<ReportHeader, List<DamagedPallet>, Map<String, String>> {
        val translatedEventDescription =
            if (header.opisZdarzenia.isNotBlank()) {
                translationService.translate(header.opisZdarzenia)
            } else {
                DescriptionGenerator.generateEnglishEventDescriptionWithTranslation(
                    pallets,
                    translationService,
                    header.reportType
                )
            }
        val translatedHeader = header.copy(
            rodzajWozka = translationService.translate(header.rodzajWozka),
            rodzajPalet = translationService.translate(header.rodzajPalet),
            rodzajSamochodu = translationService.translate(header.rodzajSamochodu),
            opisZdarzenia = translatedEventDescription
        )
        val damageTranslationMap = mutableMapOf<String, String>()
        val translatedPallets = pallets.map { originalPallet ->
            val translatedInstances = originalPallet.damageInstances.map { originalInstance ->
                val translatedDetails = originalInstance.details.map { originalDetail ->
                    val translatedTypes = originalDetail.types.map { originalType ->
                        val translatedTypeName = translationService.translate(originalType.type)
                        val translatedDamageDescription =
                            if (originalType.description.isNotBlank()) translationService.translate(
                                originalType.description
                            ) else ""
                        val originalId =
                            "${originalInstance.id}-${originalDetail.category}-${originalType.type}"
                        val translatedCategoryName =
                            translationService.translate(originalDetail.category)
                        val damageText =
                            if (originalType.type == "Inne (opis)" && translatedDamageDescription.isNotBlank()) "${translatedTypeName}: \"${translatedDamageDescription}\"" else translatedTypeName
                        val sizeText =
                            if (originalType.size.isNotBlank()) " (${originalType.size} cm)" else ""
                        val displayText = "• $translatedCategoryName: $damageText$sizeText"
                        damageTranslationMap[originalId] = displayText
                        originalType.copy(
                            type = translatedTypeName,
                            description = translatedDamageDescription
                        )
                    }.toMutableList()
                    originalDetail.copy(
                        category = translationService.translate(originalDetail.category),
                        types = translatedTypes
                    )
                }.toMutableList()
                originalInstance.copy(details = translatedDetails)
            }
            originalPallet.copy(
                rodzajTowaru = originalPallet.rodzajTowaru,
                damageInstances = translatedInstances.toMutableList()
            )
        }
        return Triple(translatedHeader, translatedPallets, damageTranslationMap)
    }

    fun logHeaderCompletion() {
        sendHeaderData()
    }

    fun setCurrentScreen(screen: Screen) {
        savedStateHandle["currentScreenRoute"] = screen.route
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun isHeaderDataValid(): Boolean {
        val header = reportHeaderState.value
        val basicValidation = _strategy.value?.isHeaderDataValid(header) ?: false
        if (!basicValidation) return false
        if (header.numerDokumentuFeniks.isNotBlank() && !CodeValidator.isValid(header.numerDokumentuFeniks)) return false
        if (header.rodzajSamochodu == "Kontener" && header.numerKontenera.isNotBlank() && !CodeValidator.isContainerNumberValid(
                header.numerKontenera
            )
        ) return false
        return true
    }

    fun updateDamageMarkers(palletId: String, markers: List<DamageMarker>) {
        val currentMap = damageMarkersState.value.toMutableMap()
        currentMap[palletId] = markers
        savedStateHandle["damageMarkers"] = currentMap
        updateSessionStateAsync("damage_markers_json", json.encodeToString(currentMap)) // SYNC
        cleanupBackgroundPdfs() // <-- DODANA LINIA
    }

    // ===== POCZĄTEK OSTATNIEJ POPRAWKI =====
    fun updatePalletDamageMarkingBitmap(palletId: String, uri: String) {
        val currentList = savedPallets.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == palletId }
        if (index != -1) {
            val updatedPallet = currentList[index].copy(damageMarkingBitmapUri = uri)
            currentList[index] = updatedPallet
            savedStateHandle["savedPallets"] = currentList

            // ===== NOWA LOGIKA SYNCHRONIZACJI =====
            // Po zapisaniu URI, musimy natychmiast zaktualizować ten model na serwerze,
            // aby był dostępny po ponownym wczytaniu sesji.
            val serverIndex = updatedPallet.serverIndex
            if (serverIndex != null) {
                updateSessionStateAsync(
                    "pallets_data.$serverIndex.full_pallet_model_json",
                    json.encodeToString(updatedPallet)
                )
            } else {
                Log.w(TAG, "Brak serverIndex dla palety $palletId, nie można zsynchronizować damageMarkingBitmapUri")
            }
            // ===== KONIEC NOWEJ LOGIKI SYNCHRONIZACJI =====
        }
    }
    // ===== KONIEC OSTATNIEJ POPRAWKI =====

    fun updateDamageHeightSelection(palletId: String, markerId: String, newSelection: Set<String>) {
        val currentPalletSelections =
            damageHeightSelections.value[palletId]?.toMutableMap() ?: mutableMapOf()
        currentPalletSelections[markerId] = newSelection
        val newMap = damageHeightSelections.value.toMutableMap()
        newMap[palletId] = currentPalletSelections
        savedStateHandle["damageHeightSelections"] = newMap
        updateSessionStateAsync("damage_heights_json", json.encodeToString(newMap)) // SYNC
        cleanupBackgroundPdfs() // <-- DODANA LINIA
    }
    fun finalizeDamageParts(palletId: String) {
        val palletSelections = damageHeightSelections.value[palletId] ?: return
        if (palletSelections.isEmpty()) return
        val allSelectedParts = palletSelections.values.flatten().toSet()
        val finalDamagePartString = allSelectedParts.sorted().joinToString(",")
        updateDamagePart(palletId, finalDamagePartString)
    }
    fun activatePlacementValidation() {
        if (!_placementValidationActive.value) {
            _placementValidationActive.value = true
        }
    }
    fun setLoggedInUserInHeader(user: UserManager.LoggedInUser) {
        val currentHeader = reportHeaderState.value
        if (currentHeader.magazynier.isBlank()) {
            savedStateHandle["reportHeader"] = currentHeader.copy(
                magazynier = user.fullName,
                workerId = user.workerId
            )
            UserManager.setCurrentUser(user.fullName)
        }
    }
    fun updateMagazynier(name: String) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(magazynier = name)
    }
    fun updateMiejsce(miejsce: String) {
        savedStateHandle["reportHeader"] =
            reportHeaderState.value.copy(miejsce = miejsce, lokalizacja = "")
    }
    fun updateLokalizacja(lokalizacja: String) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(lokalizacja = lokalizacja)
    }
    fun updateRodzajWozka(wozek: String) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(rodzajWozka = wozek)
    }
    fun updateDataGodzina(timestamp: Long) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(dataGodzina = timestamp)
    }
    fun updateRodzajPalet(rodzaj: String) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(
            rodzajPalet = rodzaj,
            rodzajSamochodu = "",
            numerAuta = "",
            numerNaczepyKontenera = ""
        )
    }
    fun updateRodzajSamochodu(rodzaj: String) {
        val currentHeader = reportHeaderState.value
        if (rodzaj != currentHeader.rodzajSamochodu) {
            clearVehicleLayout()
        }
        val updatedHeader = when (rodzaj) {
            "OKTRANS", "ROTONDO", "DSV" -> currentHeader.copy(
                rodzajSamochodu = rodzaj,
                numerAuta = "",
                numerNaczepyKontenera = ""
            )
            "Bus", "Solo" -> currentHeader.copy(
                rodzajSamochodu = rodzaj,
                numerNaczepyKontenera = ""
            )
            "Kontener" -> currentHeader.copy(rodzajSamochodu = rodzaj, numerAuta = "")
            else -> currentHeader.copy(rodzajSamochodu = rodzaj)
        }
        savedStateHandle["reportHeader"] = updatedHeader
    }
    fun updateNumerAuta(numer: String) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(numerAuta = numer)
    }
    fun updateNumerNaczepyKontenera(numer: String) {
        savedStateHandle["reportHeader"] =
            reportHeaderState.value.copy(numerNaczepyKontenera = numer)
    }
    fun updateNumerDokumentuFeniks(numer: String) {
        savedStateHandle["reportHeader"] =
            reportHeaderState.value.copy(numerDokumentuFeniks = numer)
    }
    fun updateOpisZdarzenia(opis: String) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(opisZdarzenia = opis)
        cleanupBackgroundPdfs() // <-- DODANA LINIA
    }
    fun updateNumerKontenera(numer: String) {
        savedStateHandle["reportHeader"] = reportHeaderState.value.copy(numerKontenera = numer)
    }
    fun updateSelectedVehicleLayout(layout: String?) {
        savedStateHandle["selectedVehicleLayout"] = layout

        // ===== POCZĄTEK NOWEJ POPRAWKI =====
        // Wysyłamy stan do serwera asynchronicznie
        val sessionId = currentSessionId.value
        if (sessionId != null && layout != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = directSessionApiService.updateSession(
                    sessionId,
                    mapOf("selected_vehicle_layout" to layout)
                )
                if (result.isFailure) {
                    Log.w(TAG, "Błąd synchronizacji selected_vehicle_layout: ${result.exceptionOrNull()?.message}")
                } else {
                    Log.d(TAG, "Pomyślnie zsynchronizowano selected_vehicle_layout: $layout")
                }
            }
        }
        // ===== KONIEC NOWEJ POPRAWKI =====

        cleanupBackgroundPdfs() // <-- DODANA LINIA
    }
    fun clearVehicleLayout() {
        savedStateHandle["selectedVehicleLayout"] = null
        clearAllPalletPositions()
        cleanupBackgroundPdfs() // <-- DODANA LINIA
    }
    fun clearAllPalletPositions() {
        savedStateHandle["palletPositions"] = emptyList<PalletPosition>()
        savedStateHandle["damageMarkers"] = emptyMap<String, List<DamageMarker>>()
        savedStateHandle["damageHeightSelections"] = emptyMap<String, Map<String, Set<String>>>()
        updateSessionStateAsync("pallet_positions_json", "[]")
        updateSessionStateAsync("damage_markers_json", "{}")
        updateSessionStateAsync("damage_heights_json", "{}")
        cleanupBackgroundPdfs() // <-- DODANA LINIA
    }

    // ===== POCZĄTEK OSTATNIEJ POPRAWKI (2) =====
    fun assignPalletToPosition(pallet: DamagedPallet, position: String, stackingLevel: String) {

        // ===== POPRAWKA URI (3) =====
        // Kiedy tworzymy nową pozycję, od razu skopiujmy zapisane URI obrazka,
        // aby było dostępne na następnym ekranie (PalletDetailsScreen).
        val associatedPallet = savedPallets.value.find { it.id == pallet.id }
        val bitmapUri = associatedPallet?.damageMarkingBitmapUri
        // ===== KONIEC POPRAWKI URI (3) =====

        val newPosition = PalletPosition(
            palletId = pallet.id,
            palletNumber = if (pallet.brakNumeruPalety) "BRAK NUMERU" else pallet.numerPalety,
            positionOnVehicle = position,
            stackingLevel = stackingLevel,
            damagePart = null,
            damageBitmapUri = bitmapUri // <-- Zastosowanie poprawki (3)
        )
        val currentPositions = palletPositions.value.toMutableList()
        currentPositions.removeAll { it.palletId == newPosition.palletId }
        val fullPosition = position + stackingLevel
        val conflictingPosition =
            currentPositions.find { "${it.positionOnVehicle}${it.stackingLevel}" == fullPosition }
        if (conflictingPosition != null) {
            return
        }
        currentPositions.add(newPosition)
        savedStateHandle["palletPositions"] = currentPositions
        updateSessionStateAsync("pallet_positions_json", json.encodeToString(currentPositions)) // SYNC
        cleanupBackgroundPdfs() // <-- DODANA LINIA
    }
    // ===== KONIEC OSTATNIEJ POPRAWKI (2) =====

    fun updateDamageCoordinates(palletId: String, bitmapUri: String) {
        val currentPositions = palletPositions.value.toMutableList()
        val positionIndex = currentPositions.indexOfFirst { it.palletId == palletId }
        if (positionIndex != -1) {
            val updatedPosition = currentPositions[positionIndex].copy(
                damageBitmapUri = bitmapUri
            )
            currentPositions[positionIndex] = updatedPosition
            savedStateHandle["palletPositions"] = currentPositions
            updateSessionStateAsync("pallet_positions_json", json.encodeToString(currentPositions)) // SYNC
            cleanupBackgroundPdfs() // <-- DODANA LINIA
        }
    }
    fun updateDamagePart(palletId: String, damagePart: String) {
        val currentPositions = palletPositions.value.toMutableList()
        val positionIndex = currentPositions.indexOfFirst { it.palletId == palletId }
        if (positionIndex != -1) {
            val updatedPosition = currentPositions[positionIndex].copy(damagePart = damagePart)
            currentPositions[positionIndex] = updatedPosition
            savedStateHandle["palletPositions"] = currentPositions
            updateSessionStateAsync("pallet_positions_json", json.encodeToString(currentPositions)) // SYNC
            cleanupBackgroundPdfs() // <-- DODANA LINIA
        }
    }

    // --- Logika PalletEntry ---
    private val _rawMaterialSuggestions = MutableStateFlow<List<String>>(emptyList())
    val rawMaterialSuggestions: StateFlow<List<String>> = _rawMaterialSuggestions

    fun updatePalletRodzajTowaru(rodzajTowaru: String) {
        savedStateHandle["currentPallet"] =
            currentPalletState.value.copy(rodzajTowaru = rodzajTowaru)
        if (rodzajTowaru.length >= 2) {
            _rawMaterialSuggestions.value = RawMaterialRepository.searchRawMaterials(rodzajTowaru)
        } else {
            _rawMaterialSuggestions.value = emptyList()
        }
    }
    fun clearRawMaterialSuggestions() {
        _rawMaterialSuggestions.value = emptyList()
    }
    fun onPalletNumberChange(newNumber: String) {
        updatePalletNumber(newNumber)
        barcodeFetchJob?.cancel()
        val reportType = reportHeaderState.value.reportType
        val palletType = reportHeaderState.value.rodzajPalet
        val isValidFormatForApi = CodeValidator.isAnyPalletNumberValid(newNumber)

        if (isValidFormatForApi &&
            (reportType == "Inspekcja Meiko" || reportType == "Przygotowanie wysyłki") &&
            (palletType == "Surowiec" || palletType == "Inne")
        ) {
            barcodeFetchJob = viewModelScope.launch {
                delay(750)
                fetchExternalBarcodeData(newNumber)
            }
        } else {
            _barcodeFetchState.value = BarcodeFetchState.Idle
        }
    }
    private fun fetchExternalBarcodeData(barcode: String) {
        viewModelScope.launch {
            _barcodeFetchState.value = BarcodeFetchState.Loading
            val result = externalBarcodeApiService.queryBarcode(barcode)
            result.onSuccess { response ->
                val barcodeResult = response.results[barcode]
                val rawResponse = barcodeResult?.rawResponse

                if (rawResponse != null && barcodeResult.statusCode == 200) {
                    try {
                        val lines = rawResponse.trim().split('\n').filter { it.isNotBlank() }
                        val parsedOptions = mutableListOf<BarcodeData>()

                        for (line in lines) {
                            val values = line.trim().replace("[", "").replace("]", "").split(',')
                                .map { it.trim().replace("'", "") }
                            val rodzajTowaru = values.getOrNull(12) ?: ""
                            val numerLotu = values.getOrNull(17) ?: ""
                            if (rodzajTowaru.isNotBlank() || numerLotu.isNotBlank()) {
                                parsedOptions.add(BarcodeData(rodzajTowaru = rodzajTowaru, numerLotu = numerLotu))
                            }
                        }
                        val uniqueOptions = parsedOptions.distinct()
                        when {
                            uniqueOptions.isEmpty() -> {
                                _barcodeFetchState.value = BarcodeFetchState.Error("Nie znaleziono danych (pusta odpowiedź)")
                            }
                            uniqueOptions.size == 1 -> {
                                val data = uniqueOptions.first()
                                savedStateHandle["currentPallet"] = currentPalletState.value.copy(
                                    rodzajTowaru = data.rodzajTowaru,
                                    numerLotu = data.numerLotu,
                                    brakNumeruLotu = false
                                )
                                _barcodeFetchState.value = BarcodeFetchState.Success(data)
                            }
                            else -> {
                                _barcodeFetchState.value = BarcodeFetchState.RequiresSelection(uniqueOptions)
                            }
                        }
                    } catch (e: Exception) {
                        _barcodeFetchState.value = BarcodeFetchState.Error("Błąd parsowania odpowiedzi: ${e.message}")
                    }
                } else {
                    _barcodeFetchState.value = BarcodeFetchState.Error(
                        barcodeResult?.rawResponse ?: "Brak danych dla tego kodu (status: ${barcodeResult?.statusCode})"
                    )
                }
            }.onFailure { exception ->
                _barcodeFetchState.value = BarcodeFetchState.Error(exception.message ?: "Błąd sieci")
            }
        }
    }
    fun applyBarcodeDataSelection(selectedData: List<BarcodeData>) {
        if (selectedData.isEmpty()) {
            _barcodeFetchState.value = BarcodeFetchState.Idle
            return
        }
        val productTypes = selectedData.map { it.rodzajTowaru }.distinct().joinToString(", ")
        val lotNumbers = selectedData.map { it.numerLotu }.distinct().joinToString(", ")
        savedStateHandle["currentPallet"] = currentPalletState.value.copy(
            rodzajTowaru = productTypes,
            numerLotu = lotNumbers,
            brakNumeruLotu = false
        )
        _barcodeFetchState.value = BarcodeFetchState.Idle
    }
    fun clearBarcodeFetchState() {
        _barcodeFetchState.value = BarcodeFetchState.Idle
    }

    // ===== POPRAWIONA FUNKCJA =====
    fun updatePalletNumber(number: String) {
        savedStateHandle["currentPallet"] = currentPalletState.value.copy(
            numerPalety = number.uppercase(), // <-- ZMIANA JEST TUTAJ
            brakNumeruPalety = if (number.isNotBlank()) false else currentPalletState.value.brakNumeruPalety
        )
    }
    // ================================

    fun updatePalletNumerLotu(numerLotu: String) {
        savedStateHandle["currentPallet"] = currentPalletState.value.copy(
            numerLotu = numerLotu.ifBlank { null },
            brakNumeruPalety = if (numerLotu.isNotBlank()) false else currentPalletState.value.brakNumeruPalety
        )
    }
    fun updateBrakNumeruLotu(isMissing: Boolean) {
        savedStateHandle["currentPallet"] = currentPalletState.value.copy(
            brakNumeruLotu = isMissing,
            numerLotu = if (isMissing) null else currentPalletState.value.numerLotu
        )
    }

    fun updatePalletZdjecieEtykiety(path: String?) {
        savedStateHandle["currentPallet"] = currentPalletState.value.copy(
            zdjecieDuzejEtykietyUri = path,
            zdjecieDuzejEtykietyUploadStatus = if (path != null) UploadStatus.IDLE else UploadStatus.IDLE
        )
    }

    fun updatePalletZdjecieCalejPalety(path: String?) {
        savedStateHandle["currentPallet"] = currentPalletState.value.copy(
            zdjecieCalejPaletyUri = path,
            zdjecieCalejPaletyUploadStatus = if (path != null) UploadStatus.IDLE else UploadStatus.IDLE
        )
    }

    fun addDamageInstance(photoPath: String): String {
        val current = currentPalletState.value
        val newInstance = DamageInstance(
            id = UUID.randomUUID().toString(),
            photoUri = photoPath,
            details = mutableListOf(),
            uploadStatus = UploadStatus.IDLE
        )
        val updatedInstances = current.damageInstances.toMutableList().apply {
            add(newInstance)
        }
        savedStateHandle["currentPallet"] = current.copy(damageInstances = updatedInstances)
        return newInstance.id
    }

    fun loadPalletForEditing(palletId: String) {
        val palletToEdit = savedPallets.value.find { it.id == palletId }
        if (palletToEdit != null) {
            val palletResumed = palletToEdit.copy(
                zdjecieDuzejEtykietyUploadStatus = if (palletToEdit.zdjecieDuzejEtykietyUri != null) UploadStatus.IDLE else UploadStatus.IDLE, // Zakładamy, że przy edycji stan jest IDLE
                zdjecieCalejPaletyUploadStatus = if (palletToEdit.zdjecieCalejPaletyUri != null) UploadStatus.IDLE else UploadStatus.IDLE,
                damageInstances = palletToEdit.damageInstances.map { it.copy(uploadStatus = UploadStatus.IDLE) }.toMutableList()
            )
            savedStateHandle["currentPallet"] = palletResumed
        } else {
            Log.w(TAG, "Nie znaleziono palety o ID: $palletId do edycji.")
        }
    }

    fun removeDamageInstance(instanceId: String) {
        val current = currentPalletState.value
        val updatedInstances =
            current.damageInstances.toMutableList().apply { removeAll { it.id == instanceId } }
        savedStateHandle["currentPallet"] = current.copy(damageInstances = updatedInstances)
    }

    fun updateDamageCategorySelection(
        instanceId: String,
        categoryName: String,
        isSelected: Boolean
    ) {
        val current = currentPalletState.value
        val updatedInstances = current.damageInstances.map { instance ->
            if (instance.id == instanceId) {
                val updatedDetails = instance.details.toMutableList().apply {
                    if (isSelected) {
                        if (none { it.category == categoryName }) add(DamageDetail(category = categoryName))
                    } else {
                        removeAll { it.category == categoryName }
                    }
                }
                instance.copy(details = updatedDetails)
            } else {
                instance
            }
        }
        savedStateHandle["currentPallet"] =
            current.copy(damageInstances = updatedInstances.toMutableList())
    }
    fun updateDamageTypeInfo(
        instanceId: String,
        categoryName: String,
        damageTypeName: String,
        isSelected: Boolean,
        size: String?,
        description: String?
    ) {
        val current = currentPalletState.value
        val updatedInstances = current.damageInstances.map { instance ->
            if (instance.id != instanceId) instance else {
                val updatedDetails = instance.details.map { detail ->
                    if (detail.category != categoryName) detail else {
                        val updatedTypes = detail.types.toMutableList()
                        val existingIndex = updatedTypes.indexOfFirst { it.type == damageTypeName }
                        val existingInfo =
                            if (existingIndex != -1) updatedTypes[existingIndex] else null
                        if (isSelected) {
                            val newInfo = DamageInfo(
                                type = damageTypeName,
                                size = size ?: existingInfo?.size ?: "",
                                description = description ?: existingInfo?.description ?: ""
                            )
                            if (existingIndex != -1) updatedTypes[existingIndex] =
                                newInfo else updatedTypes.add(newInfo)
                        } else {
                            if (existingIndex != -1) updatedTypes.removeAt(existingIndex)
                        }
                        detail.copy(types = updatedTypes)
                    }
                }
                instance.copy(details = updatedDetails.toMutableList())
            }
        }
        savedStateHandle["currentPallet"] =
            current.copy(damageInstances = updatedInstances.toMutableList())
    }

    private fun saveCurrentPalletToList(palletToSave: DamagedPallet) {
        val currentList = savedPallets.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == palletToSave.id }

        val palletNumber = palletToSave.numerPalety.ifBlank { "[Bez numeru]" }

        val finalPalletToSave = palletToSave.copy(
            zdjecieDuzejEtykietyUploadStatus = UploadStatus.IDLE,
            zdjecieCalejPaletyUploadStatus = UploadStatus.IDLE,
            damageInstances = palletToSave.damageInstances.map { it.copy(uploadStatus = UploadStatus.IDLE) }.toMutableList()
        )

        if (existingIndex != -1) {
            currentList[existingIndex] = finalPalletToSave
            hdmLogger.log(application, "Zaktualizowano dane dla palety: $palletNumber (Index: ${palletToSave.serverIndex}).")
        } else {
            currentList.add(finalPalletToSave)
            hdmLogger.log(
                application,
                "Dodano nową paletę ($palletNumber) do raportu. (Index: ${palletToSave.serverIndex}). Łącznie: ${currentList.size}"
            )
        }
        savedStateHandle["savedPallets"] = currentList
    }

    fun saveCurrentPallet(isFormValid: Boolean) {
        saveAndPrepareNextPallet(isFormValid)
    }

    fun resetCurrentPallet() {
        savedStateHandle["currentPallet"] = DamagedPallet()
    }
    suspend fun generateAndSavePdfToCache(context: Context): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName =
                    "Raport_HDM_${reportHeaderState.value.numerDokumentuFeniks}_${System.currentTimeMillis()}.pdf"
                val file = File(context.cacheDir, fileName)
                val reportType = reportHeaderState.value.reportType
                if (reportType.isBlank()) throw IllegalStateException("Brak typu raportu")
                val strategy = ReportStrategyFactory.create(reportType)
                val isWyrobGotowy = reportHeaderState.value.rodzajPalet == "Wyrób gotowy"

                // === POCZĄTEK POPRAWKI ===
                // Logika określająca, który layout ma zostać użyty w PDF
                val vehicleTypeFromHeader = reportHeaderState.value.rodzajSamochodu
                val layoutFromState = selectedVehicleLayoutState.value
                val isNaczepaVehicle = vehicleTypeFromHeader in listOf("Naczepa", "OKTRANS", "ROTONDO", "DSV")
                val isTandemVehicle = vehicleTypeFromHeader == "Tandem"
                val isSoloVehicle = vehicleTypeFromHeader == "Solo"
                val requiresLayoutSelection = isNaczepaVehicle || isTandemVehicle || isSoloVehicle

                // Użyj zapisanego layoutu ("3x11") jeśli był wymagany,
                // w przeciwnym razie użyj standardowego typu (np. "Bus", "Kontener")
                val finalLayoutForPdf = if (requiresLayoutSelection) {
                    layoutFromState // Użyj "3x11"
                } else {
                    vehicleTypeFromHeader // Użyj "Bus"
                }
                // === KONIEC POPRAWKI ===

                file.outputStream().use { outputStream ->
                    pdfGenerator.generatePdfReport(
                        outputStream,
                        context,
                        reportHeaderState.value,
                        savedPallets.value,
                        palletPositions.value,
                        // === ZMIANA PRZEKAZANIA PARAMETRU ===
                        finalLayoutForPdf,
                        // ====================================
                        strategy.getPdfConfig(),
                        damageMarkersState.value,
                        damageHeightSelections.value,
                        PdfReportGenerator.Language.PL,
                        strategy.getPdfTitle(),
                        isWyrobGotowy,
                        emptyMap()
                    ).getOrThrow()
                }
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    fun isReportReadyForGeneration(): Boolean {
        return true
    }
    fun resetSubmissionState() {
        _submissionState.value = SubmissionState.Idle
    }
    fun isNagoyaReportReadyForGeneration(): Boolean {
        val header = reportHeaderState.value
        val hasPallets = savedPallets.value.isNotEmpty()
        return header.magazynier.isNotBlank() &&
                header.numerDokumentuFeniks.isNotBlank() &&
                hasPallets
    }
    fun clearNameSuggestions() {}
}