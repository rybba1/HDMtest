package com.example.hdm.ui.nagoya

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.model.JapanControlReport
import com.example.hdm.model.PalletEntry
import com.example.hdm.model.ReportRepository
import com.example.hdm.model.WarehouseWorker
import com.example.hdm.network.NetworkService
import com.example.hdm.services.JapanReportSerializer
import com.example.hdm.services.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JapanReportProcessingViewModel @Inject constructor(
    private val application: Application,
    private val networkService: NetworkService,
    private val serializer: JapanReportSerializer
) : AndroidViewModel(application) {

    private val _report = MutableStateFlow<JapanControlReport?>(null)

    val reportForUi: StateFlow<JapanControlReport?> = _report.map { report ->
        report?.copy(pallets = sortPallets(report.pallets))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(ReportProcessingUiState())
    val uiState = _uiState.asStateFlow()

    private val _isReadOnly = MutableStateFlow(false)
    val isReadOnly = _isReadOnly.asStateFlow()

    private val _isMixedNewStatusMode = MutableStateFlow(false)
    val isMixedNewStatusMode = _isMixedNewStatusMode.asStateFlow()

    private val _nokPalletId = MutableStateFlow<String?>(null)

    val palletForNokEditing: StateFlow<PalletEntry?> = reportForUi.combine(_nokPalletId) { report, palletId ->
        report?.pallets?.find { it.carrierNumber == palletId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun sortPallets(pallets: List<PalletEntry>): List<PalletEntry> {
        return pallets.sortedWith(
            compareBy { pallet ->
                val status = pallet.status.uppercase()
                if (status == "OK" || status == "HDL_SEND") {
                    1
                } else {
                    0
                }
            }
        )
    }

    fun loadReport(orderId: String) {
        viewModelScope.launch {
            val originalReport = ReportRepository.getReportById(orderId)
            if (originalReport != null) {
                val isPending = originalReport.header.hdlStatus.equals("Pending", ignoreCase = true)
                val hasHdlSendPallet = originalReport.pallets.any { it.status.equals("HDL_SEND", ignoreCase = true) }
                val hasNewPallets = originalReport.pallets.any { it.status.equals("NEW", ignoreCase = true) }

                _isReadOnly.value = isPending && !hasHdlSendPallet && !hasNewPallets

                val hasOtherThanNewPallets = originalReport.pallets.any { !it.status.equals("NEW", ignoreCase = true) }
                _isMixedNewStatusMode.value = hasNewPallets && hasOtherThanNewPallets

                val reportCopy = originalReport.copy(pallets = originalReport.pallets.map { it.copy() })
                _report.value = reportCopy
                if (uiState.value.lastModifier.isNullOrBlank() && reportCopy.header.whWorker.isNotBlank()) {
                    _uiState.update { it.copy(lastModifier = reportCopy.header.whWorker) }
                }
            }
        }
    }

    fun updatePalletStatusToOk(palletToUpdate: PalletEntry) {
        updatePallet(palletToUpdate) {
            it.copy(
                status = "OK",
                whWorkerHalf = getWorkerNameOrError()
            )
        }
    }

    fun updatePalletStatusToNok(palletToUpdate: PalletEntry) {
        updatePallet(palletToUpdate) {
            it.copy(
                status = "NOK",
                whWorkerHalf = getWorkerNameOrError(),
            )
        }
    }

    fun addPhotoToPallet(palletToUpdate: PalletEntry, photoUri: Uri) {
        updatePallet(palletToUpdate) { pallet ->
            val photos = mutableListOf(pallet.nokPhoto1, pallet.nokPhoto2, pallet.nokPhoto3, pallet.nokPhoto4)
            val firstEmptySlot = photos.indexOfFirst { it.isNullOrBlank() }
            if (firstEmptySlot != -1) {
                photos[firstEmptySlot] = photoUri.toString()
            }
            pallet.copy(
                nokPhoto1 = photos.getOrNull(0),
                nokPhoto2 = photos.getOrNull(1),
                nokPhoto3 = photos.getOrNull(2),
                nokPhoto4 = photos.getOrNull(3)
            )
        }
    }

    fun removePhotoFromPallet(palletToUpdate: PalletEntry, photoUriString: String) {
        updatePallet(palletToUpdate) { pallet ->
            val photos = mutableListOf(pallet.nokPhoto1, pallet.nokPhoto2, pallet.nokPhoto3, pallet.nokPhoto4)
            val photoIndex = photos.indexOf(photoUriString)
            if (photoIndex != -1) {
                photos[photoIndex] = null
            }
            val sortedPhotos = photos.filterNotNull() + List(4 - photos.filterNotNull().size) { null }
            pallet.copy(
                nokPhoto1 = sortedPhotos.getOrNull(0),
                nokPhoto2 = sortedPhotos.getOrNull(1),
                nokPhoto3 = sortedPhotos.getOrNull(2),
                nokPhoto4 = sortedPhotos.getOrNull(3)
            )
        }
    }

    fun selectPalletForNok(pallet: PalletEntry) {
        updatePalletStatusToNok(pallet)
        _nokPalletId.value = pallet.carrierNumber
    }

    fun closeNokScreen() {
        _nokPalletId.value = null
    }

    private fun updatePallet(palletToUpdate: PalletEntry, updateAction: (PalletEntry) -> PalletEntry) {
        val currentReport = _report.value ?: return
        val updatedPallets = currentReport.pallets.map {
            if (it.carrierNumber == palletToUpdate.carrierNumber) updateAction(it) else it
        }
        val updatedReport = currentReport.copy(pallets = updatedPallets)
        _report.value = updatedReport
        viewModelScope.launch {
            ReportRepository.updateReport(getApplication(), updatedReport)
        }
        _uiState.value.selectedWorker?.let { worker ->
            _uiState.update { it.copy(lastModifier = worker.fullName) }
        }
    }

    private fun getWorkerNameOrError(): String? {
        val selectedWorker = _uiState.value.selectedWorker
        if (selectedWorker == null) {
            _uiState.update { it.copy(operationState = OperationState.Error("Błąd: Pracownik nie został wybrany!")) }
            return null
        }
        return selectedWorker.fullName
    }

    fun sendReport() {
        val currentReport = _report.value
        val finalizer = _uiState.value.selectedWorker
        if (currentReport == null || finalizer == null) {
            _uiState.update { it.copy(operationState = OperationState.Error("Musisz wybrać swoje imię i nazwisko, aby wysłać raport!")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(operationState = OperationState.Loading) }
            try {
                val finalizedPallets = currentReport.pallets.map { pallet ->
                    if (pallet.status != "NOK") {
                        pallet.copy(status = "OK")
                    } else {
                        pallet
                    }
                }
                val allPalletsAreOk = finalizedPallets.all { it.status.equals("OK", ignoreCase = true) }
                val whWorkerValue = if (allPalletsAreOk) finalizer.fullName else ""
                val reportToSend = currentReport.copy(
                    header = currentReport.header.copy(whWorker = whWorkerValue),
                    pallets = finalizedPallets
                )
                val xmlData = serializer.serialize(getApplication(), reportToSend)
                Log.d("SERIALIZER", "XML do wysłania (fragment):\n${xmlData.take(300)}")
                val result = networkService.uploadXmlReport(xmlData)
                result.onSuccess {
                    _uiState.update { it.copy(operationState = OperationState.Success("Raport wysłany pomyślnie!")) }
                    ReportRepository.reportWasSent()
                }.onFailure { exception ->
                    _uiState.update { it.copy(operationState = OperationState.Error("Błąd wysyłania: ${exception.message}")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(operationState = OperationState.Error("Krytyczny błąd: ${e.message}")) }
            }
        }
    }

    fun onWorkerSearchQueryChange(query: String) {
        _uiState.update { it.copy(workerSearchQuery = query) }
        if (query.length >= 3) {
            viewModelScope.launch {
                val allCredentials = LoginRepository.getAllCredentials(getApplication())
                val results = allCredentials
                    .filter { it.fullName.contains(query, ignoreCase = true) }
                    .map { WarehouseWorker(id = it.workerId, fullName = it.fullName) }
                _uiState.update { it.copy(workerSearchResults = results, isDropdownExpanded = results.isNotEmpty()) }
            }
        } else {
            _uiState.update { it.copy(workerSearchResults = emptyList(), isDropdownExpanded = false) }
        }
    }

    fun onWorkerSelected(worker: WarehouseWorker) {
        _uiState.update {
            it.copy(
                selectedWorker = worker,
                workerSearchQuery = worker.fullName,
                isDropdownExpanded = false
            )
        }
    }

    fun onDropdownDismiss() {
        _uiState.update { it.copy(isDropdownExpanded = false) }
    }

    fun resetOperationState() {
        _uiState.update { it.copy(operationState = OperationState.Idle) }
    }
}

data class ReportProcessingUiState(
    val workerSearchQuery: String = "",
    val selectedWorker: WarehouseWorker? = null,
    val workerSearchResults: List<WarehouseWorker> = emptyList(),
    val isDropdownExpanded: Boolean = false,
    val lastModifier: String? = null,
    val operationState: OperationState = OperationState.Idle
)

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}