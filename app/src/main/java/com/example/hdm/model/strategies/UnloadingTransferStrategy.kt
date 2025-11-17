package com.example.hdm.model.strategies

import com.example.hdm.Screen
import com.example.hdm.model.DamagedPallet
import com.example.hdm.model.ReportHeader
import com.example.hdm.services.DescriptionGenerator
import com.example.hdm.ui.header.PdfReportGenerator

class UnloadingTransferStrategy : ReportWorkflowStrategy {

    override val isVehicleSectionVisible: Boolean = true
    override val isFeniksNumberVisible: Boolean = true

    override fun getVehicleTypes(): List<String> {
        return listOf("OKTRANS", "ROTONDO", "DSV")
    }

    override fun getInitialLocationOptions(): List<String> {
        return emptyList()
    }

    override fun getScreenFlow(): List<Screen> {
        return listOf(
            Screen.ReportType, Screen.Header, Screen.PalletEntry, Screen.PalletSummary,
            Screen.PalletSelection, Screen.EventDescription, Screen.Signature
        )
    }

    override fun generatePolishEventDescription(pallets: List<DamagedPallet>, reportType: String): String {
        return DescriptionGenerator.generatePolishEventDescription(pallets, reportType)
    }

    override fun generateEnglishPalletDescription(pallet: DamagedPallet): String {
        val baseDescription = DescriptionGenerator.generateEnglishDamageKey(pallet)
        return "During transfer unloading, damage was noticed. Details: $baseDescription"
    }

    override fun getPdfTitle(): String {
        return "Rozładunek Transferu"
    }

    override fun getPdfConfig(): PdfReportGenerator.PdfConfig {
        return PdfReportGenerator.PdfConfig(
            showVehicleSection = true,
            showFeniksNumber = true,
            showPlacementSchematic = true,
            showPositionAndDamageCode = true,
            renderPalletDamageDiagram = true
        )
    }

    override fun isHeaderDataValid(header: ReportHeader): Boolean {
        return header.magazynier.isNotBlank() &&
                header.miejsce.isNotBlank() &&
                header.lokalizacja.isNotBlank() &&
                header.rodzajWozka.isNotBlank() &&
                header.rodzajPalet.isNotBlank() &&
                header.rodzajSamochodu.isNotBlank()
    }
}