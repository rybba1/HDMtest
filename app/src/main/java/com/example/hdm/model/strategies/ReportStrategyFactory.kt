package com.example.hdm.model.strategies

object ReportStrategyFactory {
    fun create(reportType: String): ReportWorkflowStrategy {
        return when (reportType) {
            "Rozładunek transferu" -> UnloadingTransferStrategy()
            "Rozładunek dostawy" -> DeliveryUnloadingStrategy()
            "Inspekcja Meiko" -> MeikoInspectionStrategy()
            "Przygotowanie wysyłki" -> ShipmentPreparationStrategy()
            else -> throw IllegalArgumentException("Nieznany typ raportu: $reportType")
        }
    }
}