package com.example.hdm.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class AttachedImage(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    var scannedBarcode: String = "",
    var note: String = "",

    val isAirFreight: Boolean = false
) : Parcelable