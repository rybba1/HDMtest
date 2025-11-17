package com.example.hdm.ui.labelprinting

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PrintRequest(
    val barcodes: List<String>,
    @SerialName("address_key")
    val addressKey: String,
    @SerialName("carrier_key")
    val carrierKey: String,
    val registration: String,
    @SerialName("printer_key")
    val printerKey: String,
    @SerialName("employee_id")
    val employeeId: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PrintResponse(
    val success: Boolean,
    val message: String
)

// NOWE: Stany procesu drukowania
sealed class PrintingState {
    object Idle : PrintingState()
    data class Processing(val step: PrintingStep, val progress: Float) : PrintingState()
    data class Success(val message: String) : PrintingState()
    data class Error(val message: String) : PrintingState()
}

enum class PrintingStep {
    VALIDATING_DATA,
    PREPARING_REQUEST,
    SENDING_TO_SERVER,
    WAITING_FOR_RESPONSE,
    FINALIZING
}