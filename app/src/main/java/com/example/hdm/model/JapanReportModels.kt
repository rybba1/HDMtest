package com.example.hdm.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Główny kontener na cały raport
@Parcelize
data class JapanControlReport(
    val header: JapanReportHeader,
    val pallets: List<PalletEntry>,
    val pdfReportBase64: String? = null // Pole na raport PDF w Base64
) : Parcelable

// Reprezentacja sekcji <Header>
@Parcelize
data class JapanReportHeader(
    var order: String = "",
    var feniks: String = "",
    var hdlStatus: String = "",
    var whWorker: String = "" // Pracownik finalizujący
) : Parcelable

// Reprezentacja pojedynczej palety <PalletEntry>
@Parcelize
data class PalletEntry(
    val carrierNumber: String,
    var status: String,
    var whWorkerHalf: String? = null, // Pracownik od NOK
    var nokPhoto1: String? = null,
    var nokPhoto2: String? = null,
    var nokPhoto3: String? = null,
    var nokPhoto4: String? = null
) : Parcelable