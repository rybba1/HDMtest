package com.example.hdm.model.strategies

import com.example.hdm.Screen
import com.example.hdm.model.*
import com.example.hdm.services.DescriptionGenerator
import com.example.hdm.ui.header.PdfReportGenerator

class DeliveryUnloadingStrategy : ReportWorkflowStrategy {
    override val isVehicleSectionVisible: Boolean = true
    override val isFeniksNumberVisible: Boolean = true

    override fun getVehicleTypes(): List<String> = listOf("Bus", "Solo", "Naczepa", "Tandem", "Kontener")
    override fun getInitialLocationOptions(): List<String> = emptyList()

    override fun getScreenFlow(): List<Screen> = listOf(
        Screen.ReportType, Screen.Header, Screen.PalletEntry, Screen.PalletSummary,
        Screen.PalletSelection, Screen.EventDescription, Screen.Signature
    )

    override fun generatePolishEventDescription(pallets: List<DamagedPallet>, reportType: String): String {
        return DescriptionGenerator.generatePolishEventDescription(pallets, reportType)
    }

    override fun generateEnglishPalletDescription(pallet: DamagedPallet): String {
        val baseDescription = DescriptionGenerator.generateEnglishDamageKey(pallet)
        return "During delivery unloading, damage was noticed. Details: $baseDescription"
    }

    override fun getPdfTitle(): String = "Rozładunek Dostawy"

    override fun getPdfConfig(): PdfReportGenerator.PdfConfig = PdfReportGenerator.PdfConfig(
        showVehicleSection = true,
        showFeniksNumber = true,
        showPlacementSchematic = true,
        showPositionAndDamageCode = true,
        renderPalletDamageDiagram = true
    )

    override fun isHeaderDataValid(header: ReportHeader): Boolean {
        return header.magazynier.isNotBlank() &&
                header.miejsce.isNotBlank() &&
                header.lokalizacja.isNotBlank() &&
                header.rodzajWozka.isNotBlank() &&
                header.rodzajPalet.isNotBlank() &&
                header.rodzajSamochodu.isNotBlank() &&
                isVehicleNumberValid(header)
    }

    private fun isVehicleNumberValid(header: ReportHeader): Boolean {
        return when (header.rodzajSamochodu) {
            "Bus", "Solo" -> header.numerAuta.isNotBlank()
            "Naczepa", "Tandem" -> header.numerAuta.isNotBlank() && header.numerNaczepyKontenera.isNotBlank()
            "Kontener" -> header.numerKontenera.isNotBlank() // ZMIENIONE: używamy nowego pola
            else -> false
        }
    }
}