package com.example.hdm.model

import android.content.Context
import android.util.Log
import com.example.hdm.services.ApiClient
import com.example.hdm.services.JapanReportParser
import com.example.hdm.services.JapanReportSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

object ReportRepository {

    private const val TAG = "JAPAN_DEBUG"
    private val parser = JapanReportParser()
    private val serializer = JapanReportSerializer()

    private val _japanControlReports = MutableStateFlow<List<JapanControlReport>>(emptyList())
    val japanControlReports = _japanControlReports.asStateFlow()

    private val _needsDelayedRefresh = MutableStateFlow(false)
    val needsDelayedRefresh = _needsDelayedRefresh.asStateFlow()

    suspend fun synchronizeAndParseReports(context: Context) {
        Log.d(TAG, "ReportRepository: Rozpoczynam synchronizację i parsowanie.")
        val reportsAsXmlStrings = ApiClient.getReports()

        if (reportsAsXmlStrings.isEmpty()) {
            Log.w(TAG, "ReportRepository: ApiClient nie zwrócił żadnych raportów do przetworzenia.")
            _japanControlReports.value = emptyList()
            return
        }

        val filesDir = context.filesDir
        filesDir.listFiles { _, name -> name.endsWith(".xml") }?.forEach { it.delete() }
        Log.d(TAG, "ReportRepository: Wyczyszczono stare pliki XML.")

        val parsedReports = reportsAsXmlStrings.mapNotNull { xmlString ->
            try {
                val report = parser.parse(xmlString)
                val file = File(filesDir, "${report.header.order}.xml")
                file.writeText(xmlString)
                report
            } catch (e: Exception) {
                Log.e(TAG, "ReportRepository: Błąd podczas parsowania lub zapisywania raportu.", e)
                null
            }
        }

        val filteredReports = parsedReports.filter { report ->
            val hasHdlStatus = report.header.hdlStatus.isNotBlank()
            val hasPallets = report.pallets.isNotEmpty()
            hasHdlStatus && hasPallets
        }

        _japanControlReports.value = filteredReports.sortedBy { it.header.order }
        Log.d(TAG, "ReportRepository: Zakończono. Sparsowano ${parsedReports.size}, po filtracji zostało ${filteredReports.size}.")
    }

    fun getReportById(orderId: String): JapanControlReport? {
        return japanControlReports.value.find { it.header.order == orderId }
    }

    suspend fun updateReport(context: Context, updatedReport: JapanControlReport) {
        withContext(Dispatchers.IO) {
            val currentList = _japanControlReports.value.toMutableList()
            val index = currentList.indexOfFirst { it.header.order == updatedReport.header.order }
            if (index != -1) {
                currentList[index] = updatedReport
                _japanControlReports.value = currentList
            }

            val updatedXmlString = serializer.serialize(context, updatedReport)
            try {
                val file = File(context.filesDir, "${updatedReport.header.order}.xml")
                file.writeText(updatedXmlString)
                Log.d(TAG, "ReportRepository: Pomyślnie nadpisano plik ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "ReportRepository: Błąd podczas nadpisywania pliku ${updatedReport.header.order}.xml", e)
            }
        }
    }

    fun reportWasSent() {
        _needsDelayedRefresh.value = true
    }

    fun delayedRefreshHandled() {
        _needsDelayedRefresh.value = false
    }
}