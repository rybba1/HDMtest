package com.example.hdm.ui.nagoya

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hdm.model.JapanControlReport
import com.example.hdm.model.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NagoyaDashboardViewModel(application: Application) : AndroidViewModel(application) {

    val reports: StateFlow<List<JapanControlReport>> = ReportRepository.japanControlReports

    // ✅ NOWOŚĆ: Stan informujący, czy trwa synchronizacja
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private var initialSyncDone = false

    /**
     * ✅ ZMIANA: Funkcja teraz ustawia flagę `isSyncing`
     * na początku i na końcu operacji.
     */
    fun syncReports() {
        // Zapobiegaj wielokrotnemu uruchomieniu, jeśli już trwa
        if (_isSyncing.value) return

        viewModelScope.launch {
            try {
                _isSyncing.value = true
                ReportRepository.synchronizeAndParseReports(getApplication())
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun initialSync() {
        if (!initialSyncDone) {
            syncReports()
            initialSyncDone = true
        }
    }
}