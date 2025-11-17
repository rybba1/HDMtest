package com.example.hdm.model.strategies

import com.example.hdm.Screen
import com.example.hdm.model.*
import com.example.hdm.services.DescriptionGenerator
import com.example.hdm.ui.header.PdfReportGenerator

class ShipmentPreparationStrategy : ReportWorkflowStrategy {

    override val isVehicleSectionVisible: Boolean = false
    override val isFeniksNumberVisible: Boolean = true

    override fun getVehicleTypes(): List<String> = emptyList()
    override fun getInitialLocationOptions(): List<String> = listOf("Lokalizacja regałowa (zeskanuj kod)")

    override fun getScreenFlow(): List<Screen> {
        return listOf(
            Screen.ReportType, Screen.Header, Screen.PalletEntry, Screen.PalletSummary,
            Screen.DamageMarkingSummary,
            Screen.EventDescription,
            Screen.Signature
        )
    }

    override fun generatePolishEventDescription(pallets: List<DamagedPallet>, reportType: String): String {
        return DescriptionGenerator.generatePolishEventDescription(pallets, reportType)
    }

    override fun generateEnglishPalletDescription(pallet: DamagedPallet): String {
        val baseDescription = DescriptionGenerator.generateEnglishDamageKey(pallet)
        return "During shipment preparation, a damaged pallet was found. Details: $baseDescription"
    }

    override fun getPdfTitle(): String = "Przygotowanie Wysyłki"

    override fun getPdfConfig(): PdfReportGenerator.PdfConfig = PdfReportGenerator.PdfConfig(
        showVehicleSection = false,
        showFeniksNumber = true,
        showPlacementSchematic = false,
        showPositionAndDamageCode = false,
        renderPalletDamageDiagram = true
    )

    override fun isHeaderDataValid(header: ReportHeader): Boolean {
        return header.magazynier.isNotBlank() &&
                header.miejsce.isNotBlank() &&
                header.lokalizacja.isNotBlank() &&
                header.rodzajWozka.isNotBlank() &&
                header.rodzajPalet.isNotBlank()
    }
}