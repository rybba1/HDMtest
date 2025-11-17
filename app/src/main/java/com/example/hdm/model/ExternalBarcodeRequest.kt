package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExternalBarcodeRequest(
    val barcodes: List<String>
)