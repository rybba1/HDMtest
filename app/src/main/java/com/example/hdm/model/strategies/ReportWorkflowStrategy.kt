package com.example.hdm.model.strategies

import com.example.hdm.Screen
import com.example.hdm.model.DamagedPallet
import com.example.hdm.model.ReportHeader
import com.example.hdm.ui.header.PdfReportGenerator

interface ReportWorkflowStrategy {

    val isVehicleSectionVisible: Boolean
    val isFeniksNumberVisible: Boolean
    fun getVehicleTypes(): List<String>
    fun getInitialLocationOptions(): List<String> // Podstawowe opcje lokalizacji

    // 2. Definicja przepływu nawigacji
    fun getScreenFlow(): List<Screen>

    // 3. Logika biznesowa (opisy)
    fun generatePolishEventDescription(pallets: List<DamagedPallet>, reportType: String): String
    fun generateEnglishPalletDescription(pallet: DamagedPallet): String // Zmieniona nazwa dla jasności

    // 4. Konfiguracja raportu PDF
    fun getPdfTitle(): String
    fun getPdfConfig(): PdfReportGenerator.PdfConfig // Zamiast wielu flag, przekażemy obiekt konfiguracyjny

    // 5. Logika walidacji
    fun isHeaderDataValid(header: ReportHeader): Boolean
}