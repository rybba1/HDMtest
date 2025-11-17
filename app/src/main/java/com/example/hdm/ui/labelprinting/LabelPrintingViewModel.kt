package com.example.hdm.ui.labelprinting

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.model.UserManager
import com.example.hdm.model.WarehouseWorker
import com.example.hdm.services.CodeValidator
import com.example.hdm.services.HdmLogger
import com.example.hdm.services.LogLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class LabelPrintingUiState(
    val selectedWorker: WarehouseWorker? = null,
    val deliveryAddress: String = "",
    val carrier: String = "",
    val customCarrierName: String = "",
    val registrationNumber: String = "",
    val scannedBarcodes: List<String> = emptyList(),
    val printer: String = "",
    val isLoading: Boolean = false,
    val printingState: PrintingState = PrintingState.Idle,
    val workerSearchQuery: String = "",
    val workerSearchResults: List<WarehouseWorker> = emptyList(),
    val isWorkerDropdownExpanded: Boolean = false,
    val validationError: String? = null
) {
    val isFormValid: Boolean
        get() = selectedWorker != null &&
                deliveryAddress.isNotBlank() &&
                carrier.isNotBlank() &&
                (carrier != "Inne" || customCarrierName.isNotBlank()) &&
                (carrier != "NGK" && carrier != "Inne" || registrationNumber.isNotBlank()) &&
                scannedBarcodes.isNotEmpty() &&
                printer.isNotBlank()
}

@HiltViewModel
class LabelPrintingViewModel @Inject constructor(
    private val application: Application,
    private val labelPrintingService: LabelPrintingService,
    private val hdmLogger: HdmLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabelPrintingUiState())
    val uiState: StateFlow<LabelPrintingUiState> = _uiState.asStateFlow()

    private val TAG = "LabelPrintingVM"
    private val json = Json { prettyPrint = true }

    // --- POCZĄTEK POPRAWKI ---
    private val _logoutAndNavigateToLogin = Channel<Unit>(Channel.CONFLATED)
    val logoutAndNavigateToLogin = _logoutAndNavigateToLogin.receiveAsFlow()
    // --- KONIEC POPRAWKI ---

    init {
        viewModelScope.launch {
            UserManager.loggedInUser.collect { user ->
                if (user != null) {
                    val worker = WarehouseWorker(id = user.workerId, fullName = user.fullName)
                    onWorkerSelected(worker)
                }
            }
        }
    }

    fun onWorkerQueryChanged(query: String) {
        _uiState.update { it.copy(workerSearchQuery = query, selectedWorker = null, workerSearchResults = emptyList(), isWorkerDropdownExpanded = false) }
    }

    fun onWorkerSelected(worker: WarehouseWorker) {
        _uiState.update {
            it.copy(
                selectedWorker = worker,
                workerSearchQuery = worker.fullName,
                isWorkerDropdownExpanded = false
            )
        }
        hdmLogger.log(application, "Druk LP: Wybrano pracownika: ${worker.fullName} (ID: ${worker.id})")
    }

    fun onWorkerDropdownDismiss() {
        _uiState.update { it.copy(isWorkerDropdownExpanded = false) }
    }

    fun onAddressSelected(address: String) {
        _uiState.update { it.copy(deliveryAddress = address) }
        hdmLogger.log(application, "Druk LP: Wybrano adres dostawy: $address")
    }

    fun onCarrierSelected(carrier: String) {
        _uiState.update { it.copy(carrier = carrier, customCarrierName = "", registrationNumber = "") }
        hdmLogger.log(application, "Druk LP: Wybrano przewoźnika: $carrier")
    }

    fun onCustomCarrierNameChanged(name: String) {
        _uiState.update { it.copy(customCarrierName = name) }
    }

    fun onRegistrationChanged(regNumber: String) {
        _uiState.update { it.copy(registrationNumber = regNumber.uppercase()) }
    }

    fun addBarcode(barcode: String) {
        if (barcode.isBlank()) {
            _uiState.update { it.copy(validationError = "Pusty kod kreskowy.") }
            return
        }
        if (_uiState.value.scannedBarcodes.contains(barcode)) {
            _uiState.update { it.copy(validationError = "Ten nośnik został już zeskanowany.") }
            return
        }

        if (CodeValidator.isAnyPalletNumberValid(barcode)) {
            _uiState.update {
                it.copy(scannedBarcodes = it.scannedBarcodes + barcode, validationError = null)
            }
            hdmLogger.log(application, "Druk LP: Zeskanowano poprawny nośnik: $barcode")
        } else {
            _uiState.update { it.copy(validationError = "Niepoprawny format nośnika: $barcode") }
            hdmLogger.log(application, "Druk LP: Odrzucono niepoprawny nośnik: $barcode", level = LogLevel.WARNING)
        }
    }

    fun clearValidationError() {
        _uiState.update { it.copy(validationError = null) }
    }

    fun removeBarcode(barcode: String) {
        _uiState.update {
            it.copy(scannedBarcodes = it.scannedBarcodes - barcode)
        }
        hdmLogger.log(application, "Druk LP: Usunięto nośnik: $barcode")
    }

    fun setPrinter(scannedName: String) {
        val transformedName = transformPrinterName(scannedName)
        _uiState.update { it.copy(printer = transformedName) }
        hdmLogger.log(application, "Druk LP: Zeskanowano kod drukarki: '$scannedName', używana nazwa: '$transformedName'")
    }

    fun sendPrintRequest() {
        val currentState = _uiState.value
        hdmLogger.log(application, "Druk LP: Kliknięto 'Wydrukuj Dokumenty'. Walidacja: ${currentState.isFormValid}")

        if (!currentState.isFormValid) {
            Log.w(TAG, "Formularz nie jest poprawny. Przerywam wysyłanie.")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(printingState = PrintingState.Processing(PrintingStep.VALIDATING_DATA, 0.2f)) }
                delay(300)

                val request = PrintRequest(
                    barcodes = currentState.scannedBarcodes,
                    addressKey = currentState.deliveryAddress,
                    carrierKey = if (currentState.carrier == "Inne") currentState.customCarrierName else currentState.carrier,
                    registration = currentState.registrationNumber,
                    printerKey = currentState.printer,
                    employeeId = "Id.${currentState.selectedWorker?.id}"
                )

                _uiState.update { it.copy(printingState = PrintingState.Processing(PrintingStep.PREPARING_REQUEST, 0.4f)) }
                delay(300)

                val jsonRequest = json.encodeToString(request)
                Log.d(TAG, "Przygotowane żądanie do wysyłki:\n$jsonRequest")
                hdmLogger.log(application, "Druk LP: Wysyłanie żądania do serwera dla przewoźnika '${request.carrierKey}' z ${request.barcodes.size} nośnikami.")

                _uiState.update { it.copy(printingState = PrintingState.Processing(PrintingStep.SENDING_TO_SERVER, 0.6f)) }
                delay(200)

                _uiState.update { it.copy(printingState = PrintingState.Processing(PrintingStep.WAITING_FOR_RESPONSE, 0.8f)) }

                val result = labelPrintingService.sendPrintRequest(request)

                _uiState.update { it.copy(printingState = PrintingState.Processing(PrintingStep.FINALIZING, 1.0f)) }
                delay(300)

                result.onSuccess { response ->
                    hdmLogger.log(application, "Druk LP: Otrzymano pomyślną odpowiedź serwera: '${response.message}'")
                    _uiState.update { it.copy(printingState = PrintingState.Success(response.message)) }

                    // --- POCZĄTEK POPRAWKI ---
                    delay(2000) // Dajemy użytkownikowi chwilę na przeczytanie sukcesu
                    UserManager.logoutUser()
                    _logoutAndNavigateToLogin.send(Unit)
                    // --- KONIEC POPRAWKI ---

                }.onFailure { exception ->
                    hdmLogger.log(application, "Druk LP: Otrzymano błąd z serwera: '${exception.message}'", level = LogLevel.ERROR)
                    _uiState.update { it.copy(printingState = PrintingState.Error(exception.message ?: "Nieznany błąd")) }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Błąd podczas wysyłania żądania", e)
                _uiState.update { it.copy(printingState = PrintingState.Error("Błąd: ${e.message}")) }
            }
        }
    }

    fun resetPrintingState() {
        _uiState.update { it.copy(printingState = PrintingState.Idle) }
    }

    fun resetForm() {
        val currentUser = _uiState.value.selectedWorker
        _uiState.value = LabelPrintingUiState(
            selectedWorker = currentUser,
            workerSearchQuery = currentUser?.fullName ?: ""
        )
        hdmLogger.log(application, "Druk LP: Zresetowano formularz.")
    }

    private fun transformPrinterName(scannedName: String): String {
        return when (scannedName.uppercase()) {
            "WH2-G" -> "Brother2_Gaudi_WH"
            "WH2G-AKWARIUM" -> "Brother_Gaudi_WH"
            "WH1-L" -> "Brother2_Leon_WH"
            "WH1-L-AKWARIUM" -> "Brother_Leon_WH"
            else -> scannedName
        }
    }
}